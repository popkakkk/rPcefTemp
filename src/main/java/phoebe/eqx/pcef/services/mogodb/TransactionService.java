package phoebe.eqx.pcef.services.mogodb;

import com.mongodb.*;
import ec02.af.utils.AFLog;
import phoebe.eqx.pcef.core.data.ResourceQuota;
import phoebe.eqx.pcef.core.data.ResourceResponse;
import phoebe.eqx.pcef.core.logs.summary.SummaryLog;
import phoebe.eqx.pcef.core.model.Quota;
import phoebe.eqx.pcef.core.model.Transaction;
import phoebe.eqx.pcef.enums.*;
import phoebe.eqx.pcef.enums.model.ETransaction;
import phoebe.eqx.pcef.enums.stats.EStatCmd;
import phoebe.eqx.pcef.enums.stats.EStatMode;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.instance.CommitData;
import phoebe.eqx.pcef.instance.Config;
import phoebe.eqx.pcef.instance.PCEFInstance;
import phoebe.eqx.pcef.message.parser.req.UsageMonitoringRequest;
import phoebe.eqx.pcef.message.parser.res.OCFUsageMonitoringResponse;
import phoebe.eqx.pcef.utils.DBResult;
import phoebe.eqx.pcef.utils.PCEFUtils;

import java.util.*;

public class TransactionService extends MongoDBService {
    public TransactionService(AppInstance appInstance, MongoClient mongoClient) {
        super(appInstance, mongoClient, Config.COLLECTION_TRANSACTION_NAME);
    }

    public void insertTransaction(String resourceId) {

        SummaryLog summaryLog;
        Transaction transaction = new Transaction();
        BasicDBObject transactionBasicObject;

        try {
            UsageMonitoringRequest usageMonitoringRequest = context.getPcefInstance().getUsageMonitoringRequest();
            transaction.setSessionId(usageMonitoringRequest.getSessionId());
            transaction.setRtid(usageMonitoringRequest.getRtid());
            transaction.setTid(usageMonitoringRequest.getTid());
            transaction.setActualTime(usageMonitoringRequest.getActualTime());
            transaction.setUserType("privateId");
            transaction.setUserValue(usageMonitoringRequest.getUserValue());
            transaction.setResourceId(resourceId);
            transaction.setResourceName(usageMonitoringRequest.getResourceName());
            transaction.setMonitoringKey("");// "" is initial
            transaction.setCounterId("");// "" is initial
            transaction.setApp(usageMonitoringRequest.getApp());

            Date now = new Date();
            transaction.setCreateDate(now);
            transaction.setUpdateDate(now);
            transaction.setStatus(EStatusLifeCycle.Waiting.getName());

            transaction.setClientId(usageMonitoringRequest.getClientId());
            transaction.setIsActive(1);

            transactionBasicObject = BasicDBObject.parse(gson.toJson(transaction));
            transactionBasicObject.put(ETransaction.updateDate.name(), transaction.getUpdateDate());
            transactionBasicObject.put(ETransaction.createDate.name(), transaction.getCreateDate());

            summaryLog = new SummaryLog(Operation.InsertTransaction.name(), new Date(), transactionBasicObject);
            context.getSummaryLogs().add(summaryLog);

            PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.sent_insert_Transaction_request);
            PCEFUtils.writeDBMessageRequest(collectionName, DBOperation.INSERT, transactionBasicObject);
        } catch (Exception e) {
            context.setPcefException(PCEFUtils.getPCEFException(e, EError.INSERT_TRANSACTION_BUILD_ERROR));
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.ERROR, EStatCmd.sent_insert_Transaction_request);
            throw e;
        }

        try {
            /**insert**/
            WriteResult writeResult = insertByQuery(transactionBasicObject);

            context.getPcefInstance().setTransaction(transaction);
            context.getPcefInstance().setInsertTransaction(true);

            BasicDBObject dbResLog = new BasicDBObject();
            dbResLog.put("_id", transactionBasicObject.get("_id"));
            dbResLog.put(ETransaction.resourceId.name(), transactionBasicObject.get(ETransaction.resourceId.name()));
            dbResLog.put(ETransaction.resourceName.name(), transactionBasicObject.get(ETransaction.resourceName.name()));
            PCEFUtils.writeDBMessageResponse(DBResult.SUCCESS, writeResult.getN(), PCEFUtils.getList(dbResLog));


            summaryLog.getSummaryLogDetail().setResponse(new Date(), dbResLog);
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.receive_insert_Transaction_response);
        } catch (DuplicateKeyException e) {
            PCEFUtils.writeDBMessageResponseError();
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.DUPPLICATE_KEY, EStatCmd.receive_insert_Transaction_response);
            context.setPcefException(PCEFUtils.getPCEFException(e, EError.INSERT_TRANSACTION_RESPONSE_DUPPLICATE_KEY));
            throw e;
        } catch (MongoTimeoutException e) {
            PCEFUtils.writeDBMessageResponseError();
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.TIMEOUT, EStatCmd.receive_insert_Transaction_response);
            context.setPcefException(PCEFUtils.getPCEFException(e, EError.INSERT_TRANSACTION_RESPONSE_TIMEOUT));
            throw e;
        } catch (MongoException e) {
            PCEFUtils.writeDBMessageResponseError();
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.ERROR, EStatCmd.receive_insert_Transaction_response);
            context.setPcefException(PCEFUtils.getPCEFException(e, EError.INSERT_TRANSACTION_RESPONSE_ERROR));
            throw e;
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

        try {
            AFLog.d("Find Transaction Refund ..");

            BasicDBObject searchQuery = new BasicDBObject();
            searchQuery.put(ETransaction.tid.name(), refId);

            ArrayList<String> inStatus = new ArrayList<>();
            inStatus.add(EStatusLifeCycle.Done.getName());
            inStatus.add(EStatusLifeCycle.Completed.getName());
            searchQuery.put(ETransaction.status.name(), new BasicDBObject("$in", inStatus));
            PCEFUtils.writeDBMessageRequest(collectionName, DBOperation.READ, searchQuery);

            DBCursor cursor = findByQuery(searchQuery);

            PCEFUtils.writeDBMessageResponse(DBResult.SUCCESS, cursor.size(), PCEFUtils.getList(cursor.iterator().next()));
            return cursor;
        } catch (MongoException e) {
            PCEFUtils.writeDBMessageResponse(DBResult.SUCCESS, 0, null);
            throw e;
        }
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
        updateQuery.put(ETransaction.updateDate.name(), new Date());

        updateSetByQuery(searchQuery, updateQuery);
    }


    public void filterTransactionErrorNewResource(OCFUsageMonitoringResponse ocfUsageMonitoringResponse, List<Transaction> newResourceTransactions, ArrayList<Quota> quotas) {
        AFLog.d("Check New Resource Request Error");
        List<String> deleteList = new ArrayList<>();

        List<Transaction> filterNewTransactions = new ArrayList<>();
        for (Transaction transaction : newResourceTransactions) {
            boolean found = false;
            boolean error = false;

            String resourceId = transaction.getResourceId();
            for (ResourceResponse resourceResponse : ocfUsageMonitoringResponse.getResources()) {
                if (resourceId.equals(resourceResponse.getResourceId())) {
                    found = true;
                    if (resourceResponse.getResultDesc().toLowerCase().contains("error")) {
                        error = true;
                        break;
                    }
                    break;
                }
            }

            if (!found || error) {
                if (!found) {
                    AFLog.d("Resource Id:" + resourceId + "not have quota return");
                }
                if (error) {
                    AFLog.d("Resource Id:" + resourceId + "return description error");
                }
                //add delete transaction
                deleteList.add(transaction.getTid());

                //filter
                filterNewTransactions.add(transaction);
            }
        }
        newResourceTransactions.removeAll(filterNewTransactions);

        //remove
        if (deleteList.size() > 0) {
            AFLog.d("Remove Transaction where receive Quota error..");
            removeManyTransactionByTid(deleteList);
        }
    }

    public void filterTransactionAndQuotaCheckUnitEnough(ArrayList<Quota> quotaResponses, List<Transaction> newResourceTransactions) {
        AFLog.d("Check Quota Not Enough");

        List<String> deleteList = new ArrayList<>();
        ArrayList<Quota> filterQuotaResponses = new ArrayList<>();

        for (Quota quota : quotaResponses) {
            int available;

            //exist quota
            if (quota.getQuotaByKey() == null) {
                List<CommitData> commitDataList = findDataToCommit(quota.getUserValue(), quota.getMonitoringKey(), false);

                if (commitDataList.size() == 0) {
                    available = 0;
                    AFLog.d("Exist Quota Response MK:" + quota.getMonitoringKey() + "Not Found");
                } else {
                    int sumTransaction = commitDataList.stream().mapToInt(CommitData::getCount).sum();
                    int quotaUnit = commitDataList.get(0).getQuotaByKey().getUnit();// spec mk
                    available = quotaUnit - sumTransaction;
                    AFLog.d("Exist Quota Response MK:" + quota.getMonitoringKey() + "available =" + available);
                }

                if (available <= 0) {
                    if (context.getRequestType().equals(ERequestType.USAGE_MONITORING)) { //flow USAGE MONITORING UPDATE ONLY
                        boolean contain = resourceIdContainQuotaResponse(context.getPcefInstance().getTransaction(), quota);
                        if (contain) {
                            newResourceTransactions.remove(0);//index 0 is  this Transaction
                            context.getPcefInstance().setSameMkExhaust(true);
                            context.getPcefInstance().setCommitDataNewList(commitDataList);
                        }
                    }
                    AFLog.d("Exist Quota Exhaust mk:" + quota.getMonitoringKey());
                    filterQuotaResponses.add(quota);
                }
            }

            //new quota
            else {
                available = quota.getQuotaByKey().getUnit();
            }

            List<Transaction> filterNewTransactions = new ArrayList<>();
            for (Transaction transaction : newResourceTransactions) {
                boolean contain = resourceIdContainQuotaResponse(transaction, quota);

                if (contain) {
                    //not enough
                    if (available <= 0) {
                        AFLog.d("Unit Not Enough ,tid:" + transaction.getTid());
                        deleteList.add(transaction.getTid());
                        filterNewTransactions.add(transaction);
                    } else {
                        //enough
                        available--;
                    }
                }
            }
            newResourceTransactions.removeAll(filterNewTransactions);
        }
        quotaResponses.removeAll(filterQuotaResponses);

        //remove
        if (deleteList.size() > 0) {
            AFLog.d("remove transaction unit not enough..");
            removeManyTransactionByTid(deleteList);
        }

    }


    private boolean resourceIdContainQuotaResponse(Transaction transaction, Quota quota) {
        boolean contain = false;

        //check contains
        for (ResourceQuota resourceResponse : quota.getResources()) {
            if (transaction.getResourceId().equals(resourceResponse.getResourceId())) {
                contain = true;
                break;
            }
        }
        return contain;
    }


    public void filterResourceRequestErrorCommitResource(OCFUsageMonitoringResponse ocfUsageMonitoringResponse, List<CommitData> commitDataList) {
        AFLog.d("Check Update Resource Request Error");
        List<CommitData> filterCommitDatas = new ArrayList<>();
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
                    AFLog.d("Resource Id:" + resourceId + "no have quota!!!");
                } else {
                    AFLog.d("Resource Id:" + resourceId + "return description error!!!");
                }

                /*if (commitData.get_id().getResourceId().equals(context.getPcefInstance().getTransaction().getResourceId())) {
                    thisTransactionSuccess = false;
                }
*/

                //remove
                AFLog.d("remove Transaction resourceId : " + resourceId);
                removeManyTransactionByTid(commitData.getTransactionIds());

                //filter
                filterCommitDatas.add(commitData);
            }

        }
        commitDataList.removeAll(filterCommitDatas);

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
                Date now = new Date();
                transaction.setMonitoringKey(resourceMap.get(transaction.getResourceId()).getMonitoringKey());
                transaction.setStatus(EStatusLifeCycle.Done.getName());
                transaction.setCounterId(resourceMap.get(transaction.getResourceId()).getCounterId());
                transaction.setUpdateDate(now);

                updateQuery.put(ETransaction.status.name(), transaction.getStatus());//waiting-->Done
                updateQuery.put(ETransaction.monitoringKey.name(), transaction.getMonitoringKey());
                updateQuery.put(ETransaction.counterId.name(), transaction.getCounterId());
                updateQuery.put(ETransaction.updateDate.name(), transaction.getUpdateDate());

                updateSetByQuery(searchQuery, updateQuery);
            }


        } catch (Exception e) {

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
