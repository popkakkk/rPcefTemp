package phoebe.eqx.pcef.services.mogodb;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;
import ec02.af.utils.AFLog;
import phoebe.eqx.pcef.core.data.ResourceRequest;
import phoebe.eqx.pcef.core.data.ResourceResponse;
import phoebe.eqx.pcef.core.model.Quota;
import phoebe.eqx.pcef.core.model.Transaction;
import phoebe.eqx.pcef.enums.ERequestType;
import phoebe.eqx.pcef.enums.EStatusLifeCycle;
import phoebe.eqx.pcef.enums.model.ETransaction;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.instance.CommitData;
import phoebe.eqx.pcef.instance.Config;
import phoebe.eqx.pcef.instance.PCEFInstance;
import phoebe.eqx.pcef.message.parser.req.UsageMonitoringRequest;
import phoebe.eqx.pcef.message.parser.res.OCFUsageMonitoringResponse;
import phoebe.eqx.pcef.utils.MessageFlow;
import phoebe.eqx.pcef.utils.PCEFUtils;

import java.util.*;

public class TransactionService extends MongoDBService {
    public TransactionService(AppInstance appInstance, MongoClient mongoClient) {
        super(appInstance, mongoClient, Config.COLLECTION_TRANSACTION_NAME);
    }

    public void insertTransaction(String resourceId) {
        try {
            UsageMonitoringRequest usageMonitoringRequest = context.getPcefInstance().getUsageMonitoringRequest();

            Transaction transaction = new Transaction();
            transaction.setSessionId(usageMonitoringRequest.getSessionId());
            transaction.setRtid(usageMonitoringRequest.getRtid());
            transaction.setTid(usageMonitoringRequest.getTid());
            transaction.setActualTime(usageMonitoringRequest.getActualTime());
            transaction.setUserType("privateId");
            transaction.setUserValue(usageMonitoringRequest.getUserValue());
            transaction.setResourceId(resourceId);
            transaction.setResourceName(usageMonitoringRequest.getResourceName());
            transaction.setMonitoringKey("");// "" is initial
            transaction.setApp(usageMonitoringRequest.getApp());

            Date now = new Date();
//            TimeZone tz = TimeZone.getTimeZone("UTC");
//            SimpleDateFormat isoDateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
//            isoDateFormatter.setTimeZone(tz);
//            System.out.println("datetime zone = "+isoDateFormatter.format(now));

            transaction.setCreateDate(now);
            transaction.setUpdateDate(now);
            transaction.setStatus(EStatusLifeCycle.Waiting.getName());

            transaction.setClientId(usageMonitoringRequest.getClientId());
            transaction.setIsActive(1);

            BasicDBObject transactionBasicObject = BasicDBObject.parse(gson.toJson(transaction));
            transactionBasicObject.put(ETransaction.updateDate.name(), transaction.getUpdateDate());
            transactionBasicObject.put(ETransaction.createDate.name(), transaction.getCreateDate());

            insertByQuery(transactionBasicObject);

            context.getPcefInstance().setTransaction(transaction);
            context.getPcefInstance().setInsertTransaction(true);

            PCEFUtils.writeMessageFlow("Insert Transaction", MessageFlow.Status.Success, context.getPcefInstance().getSessionId());
        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Insert Transaction", MessageFlow.Status.Error, context.getPcefInstance().getSessionId());

        }
    }


    public boolean findMyTransactionDone() {
        Transaction transaction = context.getPcefInstance().getTransaction();

        BasicDBObject searchQuery = new BasicDBObject();
        searchQuery.put(ETransaction.userValue.name(), transaction.getUserValue());
        searchQuery.put(ETransaction.tid.name(), transaction.getTid());
        searchQuery.put(ETransaction.status.name(), EStatusLifeCycle.Done.getName());

        return findByQuery(searchQuery).hasNext();
    }

    public DBCursor findTransactionForRefund(String refId) {
        BasicDBObject searchQuery = new BasicDBObject();
        searchQuery.put(ETransaction.tid.name(), refId);

        ArrayList<String> inStatus = new ArrayList();
        inStatus.add(EStatusLifeCycle.Done.getName());
        inStatus.add(EStatusLifeCycle.Completed.getName());

        searchQuery.put(ETransaction.status.name(), new BasicDBObject("$in", inStatus));

        return findByQuery(searchQuery);
    }


    public List<Transaction> findOtherStartTransaction(String privateId) {
        try {
            PCEFInstance pcefInstance = context.getPcefInstance();

            BasicDBObject search = new BasicDBObject();
            search.put(ETransaction.status.name(), EStatusLifeCycle.Waiting.getName());
            search.put(ETransaction.userValue.name(), privateId);
            search.put(ETransaction.monitoringKey.name(), "");//initial
            search.put(ETransaction.isActive.name(), 1);

            //Usage Monitoring add condition :{$nin:tid}
            if (context.getRequestType().equals(ERequestType.USAGE_MONITORING)) {
                ArrayList<String> tidArrayList = new ArrayList<>();
                tidArrayList.add(pcefInstance.getTransaction().getTid());
                search.put(ETransaction.tid.name(), new BasicDBObject("$nin", tidArrayList));
            }


            DBCursor otherTransactionCursor = findByQuery(search);

            List<Transaction> otherStartTransactionList = new ArrayList<>();
            while (otherTransactionCursor.hasNext()) {
                Transaction transaction = gson.fromJson(gson.toJson(otherTransactionCursor.next()), Transaction.class);
                otherStartTransactionList.add(transaction);
                AFLog.d("[find other transaction start found] tid:" + transaction.getTid() + ",resourceId:" + transaction.getResourceId());
            }

            AFLog.d("[find other transaction start found]:" + otherStartTransactionList.size());
            return otherStartTransactionList;
        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Find Other transaction error" + e.getStackTrace()[0], MessageFlow.Status.Error, context.getPcefInstance().getSessionId());
            throw e;
        }

    }


    private void updateTransactionDoneToCompleteByTid(List<CommitData> commitDataList) {
        AFLog.d("Update Transaction [Done] to [Complete] by tid..");

        BasicDBObject searchQuery = new BasicDBObject();

        List<String> tidList = new ArrayList<>();
        for (CommitData commitData : commitDataList) {
            tidList.addAll(commitData.getTransactionIds());
        }

        searchQuery.put(ETransaction.tid.name(), new BasicDBObject("$in", tidList));

        BasicDBObject updateQuery = new BasicDBObject();
        updateQuery.put(ETransaction.updateDate.name(), new Date()); //updateDate
        updateQuery.put(ETransaction.status.name(), EStatusLifeCycle.Completed.getName());

        BasicDBObject setUpdate = new BasicDBObject("$set", updateQuery);

        writeQueryLog("update", collectionName, searchQuery.toString() + "," + setUpdate.toString());
        db.getCollection(collectionName).update(searchQuery, setUpdate, false, true);
    }


    public void updateTransactionIsActive(String privateId) {
        AFLog.d("Update Transaction isActive ..");

        BasicDBObject searchQuery = new BasicDBObject();
        searchQuery.put(ETransaction.userValue.name(), privateId);
        searchQuery.put(ETransaction.status.name(), EStatusLifeCycle.Completed.getName());

        BasicDBObject updateQuery = new BasicDBObject();
        updateQuery.put(ETransaction.isActive.name(), 0);

        updateSetByQuery(searchQuery, updateQuery);
    }


    public void filterResourceRequestErrorNewResource(OCFUsageMonitoringResponse ocfUsageMonitoringResponse, List<ResourceRequest> newResourceRequests, List<Transaction> newResourceTransactions) {

        List<String> deleteList = new ArrayList<>();

        for (ResourceRequest resourceRequest : newResourceRequests) {
            boolean found = false;
            boolean error = false;
            boolean notEnough = false;

            String resourceId = resourceRequest.getResourceId();
            for (ResourceResponse resourceResponse : ocfUsageMonitoringResponse.getResources()) {
                if (resourceId.equals(resourceResponse.getResourceId())) {
                    found = true;
                    if (resourceResponse.getResultDesc().toLowerCase().contains("error")) {
                        error = true;
                        break;
                    }
                    break;
                }

                if (newResourceTransactions.size() > resourceResponse.getQuotaByKey().getUnit()) {
                    notEnough = true;
                }
            }


            if (!found || error || notEnough) {
                if (!found) {
                    AFLog.d("Resource Id:" + resourceId + "no have quota");
                }
                if (error) {
                    AFLog.d("Resource Id:" + resourceId + "return description error");
                }
                if (notEnough) {
                    AFLog.d("Resource Id:" + resourceId + "not enough");
                }

                int index = 0;
                for (Transaction transaction : newResourceTransactions) {
                    if (transaction.getResourceId().equals(resourceId)) {

                        //add delete transaction
                        deleteList.add(transaction.getTid());

                        //filter
                        newResourceTransactions.remove(index);
                    }
                    index++;
                }
            }
        }


        //remove
        if (deleteList.size() > 0) {
            AFLog.d("remove transaction..");
            removeManyTransactionByTid(deleteList);
        }

    }

    public void filterResourceRequestErrorCommitResource(OCFUsageMonitoringResponse ocfUsageMonitoringResponse, List<CommitData> commitDataList) {


        int index = 0;
        for (CommitData commitData : commitDataList) {

            if (commitData.getCount() == 0) { //rr1 : not check
                continue;
            }

            boolean found = false;
            boolean error = false;
            String resourceId = commitData.get_id().getResourceId();

            for (ResourceResponse resourceResponse : ocfUsageMonitoringResponse.getResources()) {
                if (resourceId.equals(resourceResponse.getResourceId())) {
                    found = true;
                    if (resourceResponse.getResultDesc().toLowerCase().contains("error") && resourceResponse.getResultDesc().toLowerCase().contains("commit_error")) {
                        error = true;
                        break;
                    }
                    break;
                }
            }

            if (!found || error) {
                if (!found) {
                    AFLog.d("Resource Id:" + resourceId + "no have quota");
                } else {
                    AFLog.d("Resource Id:" + resourceId + "return description error");
                }

                /*if (commitData.get_id().getResourceId().equals(context.getPcefInstance().getTransaction().getResourceId())) {
                    thisTransactionSuccess = false;
                }
*/

                //remove
                AFLog.d("remove transaction to commit by resourceId : " + resourceId);
                removeManyTransactionByTid(commitData.getTransactionIds());

                //filter
                commitDataList.remove(index);
            } else {

            }
            index++;
        }


    }


    public void removeManyTransactionByTid(List<String> tidList) {
        BasicDBObject removeQuery = new BasicDBObject();
        removeQuery.put(ETransaction.tid.name(), new BasicDBObject("$in", tidList));

        writeQueryLog("remove", collectionName, removeQuery.toString());
        db.getCollection(collectionName).remove(removeQuery);

    }


    public Map<String, Quota> resourceIdQuotaMap(ArrayList<Quota> quotas) {
        Map<String, Quota> resourceMap = new HashMap<>();
        quotas.forEach(quota ->
                quota.getResources().forEach(resourceQuota ->
                        resourceMap.put(resourceQuota.getResourceId(), quota)
                )
        );
        return resourceMap;
    }


    public void updateTransaction(ArrayList<Quota> quotas, List<Transaction> newResourceTransactions) {

        try {

            if (context.getPcefInstance().getCommitDatas().size() > 0) {
                updateTransactionDoneToCompleteByTid(context.getPcefInstance().getCommitDatas());
            }

            //get monitoring key by resource
            Map<String, Quota> resourceMap = resourceIdQuotaMap(quotas);


            //update by transaction set mk and Counter
            AFLog.d("Update transaction and other start transaction..");
            for (Transaction transaction : newResourceTransactions) {
                BasicDBObject searchQuery = new BasicDBObject();

                searchQuery.put(ETransaction.tid.name(), transaction.getTid());
                searchQuery.put(ETransaction.status.name(), EStatusLifeCycle.Waiting.getName());

                BasicDBObject updateQuery = new BasicDBObject();

                updateQuery.put(ETransaction.status.name(), EStatusLifeCycle.Done.getName());//waiting-->Done
                updateQuery.put(ETransaction.monitoringKey.name(), resourceMap.get(transaction.getResourceId()).getMonitoringKey());
                updateQuery.put(ETransaction.counterId.name(), resourceMap.get(transaction.getResourceId()).getCounterId());
                updateQuery.put(ETransaction.updateDate.name(), new Date());

                updateSetByQuery(searchQuery, updateQuery);
            }

            PCEFUtils.writeMessageFlow("Update Transaction", MessageFlow.Status.Success, context.getPcefInstance().getSessionId());
        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Update Transaction error" + e.getStackTrace()[0], MessageFlow.Status.Error, context.getPcefInstance().getSessionId());
            throw e;
        }
    }


    public void updateTransactionWaitingToComplete(ArrayList<Quota> quotas) {
        AFLog.d("Update My Transaction Complete..");
        try {
            //get monitoring key by resource
            Transaction transaction = context.getPcefInstance().getTransaction();

            Map<String, Quota> resourceMap = resourceIdQuotaMap(quotas);
            BasicDBObject searchQuery = new BasicDBObject();

            searchQuery.put(ETransaction.tid.name(), transaction.getTid());
            searchQuery.put(ETransaction.status.name(), EStatusLifeCycle.Waiting.getName());

            BasicDBObject updateQuery = new BasicDBObject();
            updateQuery.put(ETransaction.status.name(), EStatusLifeCycle.Completed.getName());//waiting -->Complete
            updateQuery.put(ETransaction.monitoringKey.name(), resourceMap.get(transaction.getResourceId()).getMonitoringKey());
            updateQuery.put(ETransaction.counterId.name(), resourceMap.get(transaction.getResourceId()).getCounterId());
            updateQuery.put(ETransaction.updateDate.name(), new Date());
            updateSetByQuery(searchQuery, updateQuery);
        } catch (Exception e) {
            AFLog.d("Update My Transaction Complete Error -" + e.getStackTrace()[0]);
            throw e;
        }
    }


    public void deleteTransactionError() {
        if (appInstance.getMyContext().getPcefInstance().isInsertTransaction()) {
            AFLog.d("Error!!--> Delete Transaction Error..");
            deleteTransactionByTid(appInstance.getMyContext().getPcefInstance().getTransaction().getTid());
        }
    }


    public void deleteTransactionByTid(String tid) {
        BasicDBObject delete = new BasicDBObject(ETransaction.tid.name(), tid);
        writeQueryLog("delete", collectionName, delete.toString());
        db.getCollection(collectionName).remove(delete);
    }


}
