package phoebe.eqx.pcef.services.mogodb;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import phoebe.eqx.pcef.core.model.Quota;
import phoebe.eqx.pcef.core.model.Transaction;
import phoebe.eqx.pcef.enums.ERequestType;
import phoebe.eqx.pcef.enums.EStatusLifeCycle;
import phoebe.eqx.pcef.enums.model.ETransaction;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.instance.Config;
import phoebe.eqx.pcef.instance.PCEFInstance;
import phoebe.eqx.pcef.message.parser.req.UsageMonitoringRequest;
import phoebe.eqx.pcef.utils.MessageFlow;
import phoebe.eqx.pcef.utils.PCEFUtils;

import java.util.*;

public class TransactionService extends MongoDBService {
    public TransactionService(AppInstance appInstance, MongoClient mongoClient) {
        super(appInstance, mongoClient, Config.COLLECTION_TRANSACTION_NAME);
    }

    public void insertTransaction(String resourceId) {
        try {
            UsageMonitoringRequest usageMonitoringRequest = appInstance.getPcefInstance().getUsageMonitoringRequest();

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
            transaction.setCreateDate(now);
            transaction.setUpdateDate(now);
            transaction.setStatus(EStatusLifeCycle.Waiting.getName());

            transaction.setClientId(usageMonitoringRequest.getClientId());
            transaction.setIsActive(1);


            BasicDBObject transactionBasicObject = BasicDBObject.parse(gson.toJson(transaction));
            transactionBasicObject.put(ETransaction.updateDate.name(), transaction.getUpdateDate());
            transactionBasicObject.put(ETransaction.createDate.name(), transaction.getCreateDate());

            insertByQuery(transactionBasicObject);

            appInstance.getPcefInstance().setTransaction(transaction);
            PCEFUtils.writeMessageFlow("Insert Transaction", MessageFlow.Status.Success, appInstance.getPcefInstance().getSessionId());
        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Insert Transaction", MessageFlow.Status.Error, appInstance.getPcefInstance().getSessionId());

        }
    }


    public boolean findMyTransactionDone() {
        Transaction transaction = appInstance.getPcefInstance().getTransaction();

        BasicDBObject searchQuery = new BasicDBObject();
        searchQuery.put(ETransaction.userValue.name(), transaction.getUserValue());
        searchQuery.put(ETransaction.tid.name(), transaction.getTid());
        searchQuery.put(ETransaction.status.name(), EStatusLifeCycle.Done.getName());

        return findByQuery(searchQuery).hasNext();
    }


    public List<Transaction> findOtherStartTransaction() {
        try {
            PCEFInstance pcefInstance = appInstance.getPcefInstance();

            BasicDBObject search = new BasicDBObject();
            search.put(ETransaction.status.name(), EStatusLifeCycle.Waiting.getName());
            search.put(ETransaction.userValue.name(), appInstance.getPcefInstance().getProfile().getUserValue());
            search.put(ETransaction.monitoringKey.name(), "");//initial
            search.put(ETransaction.isActive.name(), 1);

            //Usage Monitoring add condition :{$nin:tid}
            if (appInstance.getRequestType().equals(ERequestType.USAGE_MONITORING)) {
                ArrayList<String> tidArrayList = new ArrayList<>();
                tidArrayList.add(pcefInstance.getTransaction().getTid());
                search.put(ETransaction.tid.name(), new BasicDBObject("$nin", tidArrayList));
            }


            DBCursor otherTransactionCursor = findByQuery(search);

            List<Transaction> otherStartTransactionList = new ArrayList<>();
            while (otherTransactionCursor.hasNext()) {
                Transaction transaction = gson.fromJson(gson.toJson(otherTransactionCursor.next()), Transaction.class);
                otherStartTransactionList.add(transaction);
            }


            return otherStartTransactionList;
        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Find Profile by privateId", MessageFlow.Status.Error, appInstance.getPcefInstance().getSessionId());
            throw e;
        }

    }

    private void updateTransactionDoneToComplete(Quota quota) {
        BasicDBObject searchQuery = new BasicDBObject();

        searchQuery.put(ETransaction.monitoringKey.name(), quota.getMonitoringKey());
        searchQuery.put(ETransaction.userValue.name(), quota.getUserValue());
        searchQuery.put(ETransaction.status.name(), EStatusLifeCycle.Done.getName());

        BasicDBObject updateQuery = new BasicDBObject();
        updateQuery.put(ETransaction.updateDate.name(), new Date()); //updateDate
        updateQuery.put(ETransaction.status.name(), EStatusLifeCycle.Completed.getName());
        db.getCollection(collectionName).update(searchQuery, new BasicDBObject("$set", updateQuery), false, true);
    }


    public void updateTransactionIsActive(String privateId) {
        BasicDBObject searchQuery = new BasicDBObject();
        searchQuery.put(ETransaction.userValue.name(), privateId);
        searchQuery.put(ETransaction.status.name(), EStatusLifeCycle.Completed.getName());

        BasicDBObject updateQuery = new BasicDBObject();
        updateQuery.put(ETransaction.isActive.name(), 0);

        updateSetByQuery(searchQuery, updateQuery);
    }


    public void updateTransaction(ArrayList<Quota> quotas) {

        List<Transaction> updateTransactionList = new ArrayList<>();

        //commit -->Update Status Done to Complete
        if (appInstance.getPcefInstance().doCommit()) {
            Quota quotaExhaust = appInstance.getPcefInstance().getCommitPart().getQuotaExhaust();
            List<Quota> quotaExpireList = appInstance.getPcefInstance().getCommitPart().getQuotaExpireList();
            if (quotaExhaust != null) {
                updateTransactionDoneToComplete(quotaExhaust);
            } else if (quotaExpireList.size() > 0) {
                for (Quota quota : quotaExpireList) {
                    updateTransactionDoneToComplete(quota);
                }
            }
        }

        //get monitoring key by resource
        Map<String, Quota> resourceMap = new HashMap<>();
        quotas.forEach(quota ->
                quota.getResources().forEach(resourceQuota ->
                        resourceMap.put(resourceQuota.getResourceId(), quota)
                )
        );

        if (ERequestType.USAGE_MONITORING.equals(appInstance.getRequestType())) {
            updateTransactionList.add(appInstance.getPcefInstance().getTransaction());
        }

        updateTransactionList.addAll(appInstance.getPcefInstance().getOtherStartTransactions());

        //update by transaction set mk and Counter
        for (Transaction transaction : updateTransactionList) {
            BasicDBObject searchQuery = new BasicDBObject();

            searchQuery.put(ETransaction.tid.name(), transaction.getTid());
            searchQuery.put(ETransaction.status.name(), EStatusLifeCycle.Waiting.getName());

            BasicDBObject updateQuery = new BasicDBObject();
            if (appInstance.getPcefInstance().doCommit() && appInstance.getPcefInstance().getTransaction().getTid().equals(transaction.getTid())) {
                updateQuery.put(ETransaction.status.name(), EStatusLifeCycle.Completed.getName());//waiting -->Complete
            } else {
                updateQuery.put(ETransaction.status.name(), EStatusLifeCycle.Done.getName());//waiting-->Done
            }
            updateQuery.put(ETransaction.monitoringKey.name(), resourceMap.get(transaction.getResourceId()).getMonitoringKey());
            updateQuery.put(ETransaction.counterId.name(), resourceMap.get(transaction.getResourceId()).getCounterId());
            updateQuery.put(ETransaction.updateDate.name(), new Date());

            updateSetByQuery(searchQuery, updateQuery);
        }
    }


    public Map<String, Integer> findTransactionDoneGroupByResource(List<Quota> quotaList) {

        Map<String, Integer> countUnitByResourceMap = new HashMap<>();

        for (Quota quota : quotaList) {

            BasicDBObject match = new BasicDBObject();
            match.put(ETransaction.monitoringKey.name(), quota.getMonitoringKey());
            match.put(ETransaction.status.name(), EStatusLifeCycle.Done.getName());

            BasicDBObject group = new BasicDBObject();
            group.put("_id", "$" + ETransaction.resourceId.name());
            group.put("count", new BasicDBObject("$sum", 1));

            Iterator<DBObject> transactionGroupByResourceIterator = aggregateMatch(match, group).iterator();
            while (transactionGroupByResourceIterator.hasNext()) {
                DBObject dbObject = transactionGroupByResourceIterator.next();

                int count = (int) Double.parseDouble(dbObject.get("count").toString());
                String resourceId = dbObject.get("_id").toString();

                if (ERequestType.USAGE_MONITORING.equals(appInstance.getRequestType())) {
                    if (appInstance.getPcefInstance().getTransaction().getResourceId().equals(resourceId)) {
                        count += 1;//resource of this transaction
                    }
                }
                countUnitByResourceMap.put(resourceId, count);
            }
        }


        return countUnitByResourceMap;
    }
}
