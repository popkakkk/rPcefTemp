package phoebe.eqx.pcef.services.mogodb;

import com.google.gson.Gson;
import com.mongodb.*;
import com.mongodb.client.MongoDatabase;
import ec02.af.utils.AFLog;
import phoebe.eqx.pcef.core.data.ResourceResponse;
import phoebe.eqx.pcef.core.data.UsageMonitoring;
import phoebe.eqx.pcef.core.data.ResourceQuota;
import phoebe.eqx.pcef.enums.EStatusLifeCycle;
import phoebe.eqx.pcef.enums.model.EProfile;
import phoebe.eqx.pcef.enums.model.EQuota;
import phoebe.eqx.pcef.enums.model.ETransaction;
import phoebe.eqx.pcef.enums.model.element.EResourceQuota;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.instance.Config;
import phoebe.eqx.pcef.instance.PCEFInstance;
import phoebe.eqx.pcef.message.parser.req.UsageMonitoringRequest;
import phoebe.eqx.pcef.core.model.Profile;
import phoebe.eqx.pcef.core.model.Quota;
import phoebe.eqx.pcef.core.model.Transaction;
import phoebe.eqx.pcef.utils.MessageFlow;
import phoebe.eqx.pcef.utils.PCEFUtils;

import java.util.*;

public class MongoDBService {

    private AppInstance appInstance;
    private MongoClient mongoClient;
    private DB db;
    private MongoDatabase mongoDatabase;
    private Gson gson = new Gson();


    private Date minExpireDate;
    private boolean haveNewQuota;


    //---- Connect DB-----------------------------------------------------
    public MongoDBService(AppInstance appInstance) {
        this.appInstance = appInstance;
        this.connectDB(Config.MONGODB_URL, Config.MY_DB_NAME);
    }

    private void connectDB(String url, String dbName) {
        mongoClient = new MongoClient(new MongoClientURI(url));
        this.mongoDatabase = mongoClient.getDatabase(dbName);
        this.db = mongoClient.getDB(dbName);
    }

    public void closeConnection() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    //-----Basic Function---------------------------------------------------
    private void insertByObject(String collectionName, Object object) {
        String json = gson.toJson(object);
        writeQueryLog("insert", collectionName, json);
        db.getCollection(collectionName).insert(BasicDBObject.parse(json));
    }

    private void insertByQuery(String collectionName, BasicDBObject basicDBObject) {
        db.getCollection(collectionName).insert(basicDBObject);
    }

    private void insertManyByObject(String collectionName, List<BasicDBObject> insertList) {
        writeQueryLog("insert many", collectionName, gson.toJson(insertList));
        db.getCollection(collectionName).insert(insertList);
    }

    private void updatePush(String collectionName, BasicDBObject searchQuery, BasicDBObject updateQuery) {
        BasicDBObject pushUpdate = new BasicDBObject("$push", updateQuery);
        String condition = "searchQuery:" + searchQuery.toJson() + ",updateQuery:" + pushUpdate.toJson();
        writeQueryLog("update", collectionName, condition);
        db.getCollection(collectionName).update(searchQuery, pushUpdate);
    }


    private DBCursor findByObject(String collectionName, Object object) {
        String json = gson.toJson(object);
        writeQueryLog("find", collectionName, json);
        return db.getCollection(collectionName).find(BasicDBObject.parse(json));
    }

    private DBCursor findByQuery(String collectionName, BasicDBObject whereQuery) {
        writeQueryLog("find", collectionName, whereQuery.toJson());
        return db.getCollection(collectionName).find(whereQuery);
    }

    private void updateSetByQuery(String collectionName, BasicDBObject searchQuery, BasicDBObject updateQuery) {
        BasicDBObject setUpdate = new BasicDBObject("$set", updateQuery);
        String condition = "searchQuery:" + searchQuery.toJson() + ",updateQuery:" + setUpdate.toJson();
        writeQueryLog("update", collectionName, condition);
        db.getCollection(collectionName).update(searchQuery, setUpdate);
    }


    private Iterable<DBObject> aggregateMatch(String collectionName, BasicDBObject match, BasicDBObject group) {
        List<DBObject> pipeline = Arrays.asList(
                new BasicDBObject("$match", match)
                , new BasicDBObject("$group", group));

        String condition = "$match:" + match.toJson() + ",$group:" + group.toJson();
        writeQueryLog("aggregate", collectionName, condition);
        return db.getCollection(collectionName).aggregate(pipeline).results();
    }

    private DBObject findAndModify(String collectionName, BasicDBObject query, BasicDBObject update) {
        BasicDBObject setUpdate = new BasicDBObject("$set", update);
        return db.getCollection(collectionName).findAndModify(query, setUpdate);
    }


    private List distinctByObject(String collectionName, String fieldName, Object object) {
        return db.getCollection(collectionName).distinct(fieldName, BasicDBObject.parse(gson.toJson(object)));
    }


    //---- Transaction function---------------------------------------------------------------------------------------
    public void insertTransaction(String resourceId) {
        try {
            UsageMonitoringRequest usageMonitoringRequest = appInstance.getPcefInstance().getUsageMonitoringRequest();

            Transaction transaction = new Transaction();
            transaction.setSessionId(usageMonitoringRequest.getSessionId());
            transaction.setRtid(usageMonitoringRequest.getRtid());
            transaction.setTid(usageMonitoringRequest.getTid());
            transaction.setActualTime(usageMonitoringRequest.getActualTime());
            transaction.setUserType("privateId");
            transaction.setUserValue(usageMonitoringRequest.getPrivateId());
            transaction.setResourceId(resourceId);
            transaction.setResourceName(usageMonitoringRequest.getResourceName());
            transaction.setMonitoringKey("");// "" is initial

            Date now = new Date();
            transaction.setCreateDate(now);
            transaction.setUpdateDate(now);
            transaction.setStatus(EStatusLifeCycle.Waiting.getName());

            transaction.setClientId(usageMonitoringRequest.getClientId());
            transaction.setFirstTime(1);
            transaction.setIsActive(1);

            insertByObject(Config.COLLECTION_TRANSACTION_NAME, transaction);

            appInstance.getPcefInstance().setTransaction(transaction);
            PCEFUtils.writeMessageFlow("Insert Transaction", MessageFlow.Status.Success, appInstance.getPcefInstance().getSessionId());
        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Insert Transaction", MessageFlow.Status.Error, appInstance.getPcefInstance().getSessionId());

        }
    }



    public DBCursor findTransactionByStatus(String status) {
        String privateId = appInstance.getPcefInstance().getUsageMonitoringRequest().getPrivateId();
        DBCollection collection = db.getCollection(Config.COLLECTION_TRANSACTION_NAME);
        BasicDBObject searchQuery = new BasicDBObject();
        searchQuery.put(ETransaction.userValue.name(), privateId);
        return collection.find(searchQuery);
    }


    public boolean firstTimeAndWaitProcessing() {
        return true;
    }


    public void findOtherStartTransaction() {
        try {

            PCEFInstance pcefInstance = appInstance.getPcefInstance();

            BasicDBObject match = new BasicDBObject();
            match.put(ETransaction.status.name(), EStatusLifeCycle.Waiting.getName());
            match.put(ETransaction.userValue.name(), pcefInstance.getUsageMonitoringRequest().getPrivateId());
            match.put(ETransaction.monitoringKey.name(), "");//initial

            List<String> resourceIdThisTransaction = new ArrayList<>();
            resourceIdThisTransaction.add(pcefInstance.getTransaction().getResourceId());
            match.put(ETransaction.resourceId.name(), new BasicDBObject("$nin", resourceIdThisTransaction));//resourceId must != this.transaction.resourceId

            BasicDBObject group = new BasicDBObject();
            group.put(ETransaction.resourceId.name(), "$" + ETransaction.resourceId.name());
            group.put(ETransaction.resourceName.name(), "$" + ETransaction.resourceName.name());
            group.put(ETransaction.rtid.name(), "$" + ETransaction.rtid.name());
            group.put(ETransaction.tid.name(), "$" + ETransaction.tid.name());

            Iterator<DBObject> iterator = aggregateMatch(Config.COLLECTION_TRANSACTION_NAME, match, new BasicDBObject("_id", group)).iterator();

            List<Transaction> otherStartTransactionList = new ArrayList<>();
            while (iterator.hasNext()) {
                DBObject dbObject = iterator.next();
                DBObject result = (DBObject) dbObject.get("_id");

                Transaction transaction = new Transaction();
                transaction.setResourceId(result.get(ETransaction.resourceId.name()).toString());
                transaction.setResourceName(result.get(ETransaction.resourceId.name()).toString());
                transaction.setTid(result.get(ETransaction.tid.name()).toString());
                transaction.setRtid(result.get(ETransaction.rtid.name()).toString());
                otherStartTransactionList.add(transaction);
            }
            pcefInstance.getOtherStartTransactions().addAll(otherStartTransactionList);

        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Find Profile by privateId", MessageFlow.Status.Error, appInstance.getPcefInstance().getSessionId());
            throw e;
        }


    }

    public void updateTransactionFirstTime() {

    }

    //---- Profile function---------------------------------------------------------------------------------------------
    public void insertProfile() {
        try {
            UsageMonitoringRequest usageMonitoringRequest = appInstance.getPcefInstance().getUsageMonitoringRequest();
            Profile profile = new Profile();
            profile.set_id(usageMonitoringRequest.getPrivateId());
            profile.setUserType("privateId");
            profile.setUserValue(usageMonitoringRequest.getPrivateId());
            profile.setIsProcessing(1);
            profile.setSequenceNumber(1);


            insertByObject(Config.COLLECTION_PROFILE_NAME, profile);
            PCEFUtils.writeMessageFlow("Insert Profile", MessageFlow.Status.Success, appInstance.getPcefInstance().getSessionId());
        } catch (DuplicateKeyException e) {
            PCEFUtils.writeMessageFlow("Insert Profile Duplicate Key", MessageFlow.Status.Error, appInstance.getPcefInstance().getSessionId());
            throw e;
        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Insert Profile", MessageFlow.Status.Error, appInstance.getPcefInstance().getSessionId());
            throw e;
        }
    }


    public boolean checkCanProcessProfile(DBCursor lockProcessCursor) {
        DBObject dbObject = lockProcessCursor.next();
        String isProcessing = String.valueOf(dbObject.get(EProfile.isProcessing.name()));
        return isProcessing.equals("0");
    }


    public DBCursor findProfileByPrivateId() {

        try {
            String privateId = appInstance.getPcefInstance().getUsageMonitoringRequest().getPrivateId();
            BasicDBObject searchQuery = new BasicDBObject();
            searchQuery.put(EProfile.userValue.name(), privateId);

            DBCursor dbCursor = findByQuery(Config.COLLECTION_PROFILE_NAME, searchQuery);


            if (dbCursor.hasNext()) {
                DBObject profileDbObject = dbCursor.iterator().next();

                Date appointmentDate = (Date) profileDbObject.get(EProfile.appointmentDate.name());
                String sessionId = (String) profileDbObject.get(EProfile.sessionId.name());

                appInstance.getPcefInstance().setAppointmentDate(appointmentDate);
                appInstance.getPcefInstance().setOcfSessionId(sessionId);

                PCEFUtils.writeMessageFlow("Find Profile by privateId [Found]", MessageFlow.Status.Success, appInstance.getPcefInstance().getSessionId());
            } else {
                PCEFUtils.writeMessageFlow("Find Profile by privateId [Not Found]", MessageFlow.Status.Success, appInstance.getPcefInstance().getSessionId());
            }
            return dbCursor;
        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Find Profile by privateId", MessageFlow.Status.Error, appInstance.getPcefInstance().getSessionId());
            throw e;
        }
    }

    private void updateProfileUnlock(BasicDBObject updateQuery) {
        updateQuery.put(EProfile.isProcessing.name(), 0);//unlock

        BasicDBObject searchQuery = new BasicDBObject();
        searchQuery.put(EProfile.userValue.name(), appInstance.getPcefInstance().getTransaction().getUserValue());
        searchQuery.put(EProfile.isProcessing.name(), 1);

        updateSetByQuery(Config.COLLECTION_PROFILE_NAME, searchQuery, updateQuery);
    }


    public void updateProfileUnLockInitial() {
        BasicDBObject updateQuery = new BasicDBObject();
        updateQuery.put(EProfile.appointmentDate.name(), minExpireDate);
        updateQuery.put(EProfile.sessionId.name(), appInstance.getPcefInstance().getOcfSessionId());
        updateProfileUnlock(updateQuery);
    }


    public void updateProfileUnLock() {
        BasicDBObject updateQuery = new BasicDBObject();
        if (haveNewQuota) {
            if (minExpireDate.before(appInstance.getPcefInstance().getAppointmentDate())) {
                updateQuery.put(EProfile.appointmentDate.name(), minExpireDate);
//                appInstance.getPcefInstance().setAppointmentDate(minExpireDate);
            }
        }
        updateProfileUnlock(updateQuery);
    }

    public DBObject findAndModifyLockProfile() {

        String privateId = appInstance.getPcefInstance().getTransaction().getUserValue();
        BasicDBObject query = new BasicDBObject();
        query.put(EProfile.userValue.name(), privateId);
        query.put(EProfile.isProcessing.name(), 0);

        BasicDBObject update = new BasicDBObject();
        update.put(EProfile.isProcessing.name(), 1);//lock

        return findAndModify(Config.COLLECTION_PROFILE_NAME, query, update);
    }

    public DBObject findAndModifyLockQuota(String monitoringKey) {
        BasicDBObject query = new BasicDBObject();
        query.put(EQuota._id.name(), monitoringKey);
        query.put(EQuota.processing.name(), 0);

        BasicDBObject update = new BasicDBObject();
        update.put(EQuota.processing.name(), 1);//lock

        return findAndModify(Config.COLLECTION_QUOTA_NAME, query, update);
    }


    //---- Quota function----------------------------------------------------------------------------------------------

    public DBCursor findQuotaByResource() {
        Transaction transaction = appInstance.getPcefInstance().getTransaction();
        BasicDBObject searchQuery = new BasicDBObject();
        searchQuery.put(EQuota.userValue.name(), transaction.getUserValue());
        searchQuery.put(EQuota.resources.name(), new BasicDBObject("$elemMatch", new BasicDBObject("resourceId", transaction.getResourceId())));
        return findByQuery(Config.COLLECTION_QUOTA_NAME, searchQuery);
    }

    public boolean checkMkCanProcess(DBCursor monitoringKeyCursor) {
        DBObject dbObject = monitoringKeyCursor.iterator().next();
        String processing = String.valueOf(dbObject.get(EQuota.processing.name()));
        return processing.equals("0");
    }


    public ArrayList<Quota> getQuotaFromUsageMonitoringResponse(UsageMonitoring usageMonitoringResponse) {
        Map<String, Quota> quotaMap = new HashMap<>();
        for (ResourceResponse resourceResponse : usageMonitoringResponse.getResourceResponses()) {
            String monitoringKey = resourceResponse.getMonitoringKey();
            String resourceName = resourceResponse.getResourceName();
            String resourceId = resourceResponse.getResourceId();

            ResourceQuota resourceQuota = new ResourceQuota();
            resourceQuota.setResourceId(resourceId);
            resourceQuota.setResourceName(resourceName);

            Quota myQuota = quotaMap.get(monitoringKey);
            if (myQuota == null) {
                Quota quota = new Quota();
                quota.set_id(resourceResponse.getMonitoringKey());
                quota.setUserType(usageMonitoringResponse.getUserType());
                quota.setUserValue(usageMonitoringResponse.getUserValue());
                quota.setProcessing("0");
                quota.setMonitoringKey(resourceResponse.getMonitoringKey());
                quota.setCounterId(resourceResponse.getCounterId());
                quota.setQuotaByKey(resourceResponse.getQuotaByKey());
                quota.setRateLimitByKey(resourceResponse.getRateLimitByKey());
                quota.getResources().add(resourceQuota);
                quotaMap.put(monitoringKey, quota);
            } else {
                //Same Quota --> update resourceResponse
                myQuota.getResources().add(resourceQuota);
            }
        }
        return new ArrayList<>(quotaMap.values());
    }


    private List<BasicDBObject> setExpireDate(ArrayList<Quota> quotaArrayList) {
        List<BasicDBObject> quotaBasicObjectList = new ArrayList<>();
        for (Quota quota : quotaArrayList) {
            BasicDBObject basicDBObject = BasicDBObject.parse(gson.toJson(quota));

            if (quota.getQuotaByKey() != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.MINUTE, Integer.parseInt(quota.getQuotaByKey().getValidityTime()));
                basicDBObject.put(EQuota.expireDate.name(), calendar.getTime());
            }

            quotaBasicObjectList.add(basicDBObject);
        }
        return quotaBasicObjectList;
    }


    public void insertQuotaStartFirstUsage(ArrayList<Quota> quotaArrayList) {
        List<BasicDBObject> quotaBasicObjectList = setExpireDate(quotaArrayList);
        insertManyByObject(Config.COLLECTION_QUOTA_NAME, quotaBasicObjectList);
        this.minExpireDate = findMinExpireDate(quotaBasicObjectList);
    }

    public Date findMinExpireDate(List<BasicDBObject> quotaBasicObjectList) {
        Date minDate = null;
        for (BasicDBObject basicDBObject : quotaBasicObjectList) {
            Date date = (Date) basicDBObject.get(EQuota.expireDate.name());
            if (minDate != null) {
                if (date.before(minDate)) {
                    minDate = date;
                }
            } else {
                minDate = date;
            }
        }
        return minDate;
    }


    public void updateQuota(ArrayList<Quota> quotaResponses) {

        List<BasicDBObject> quotaBasicObjectList = setExpireDate(quotaResponses);
        List<BasicDBObject> newQuotaList = new ArrayList<>();

        for (BasicDBObject quotaBasicObject : quotaBasicObjectList) {
            String newMk = quotaBasicObject.get(EQuota.monitoringKey.name()).toString();

            if (quotaBasicObject.get(EQuota.quotaByKey.name()) != null) {
                //new quota --> insert
                insertByQuery(Config.COLLECTION_QUOTA_NAME, quotaBasicObject);
                newQuotaList.add(quotaBasicObject);

                //remove old quota
                if (appInstance.getPcefInstance().isQuotaExhaust()) {
                    String oldMk = appInstance.getPcefInstance().getQuotaCommit().getMonitoringKey();

                    if (!oldMk.equals(newMk)) {
                        BasicDBObject search = new BasicDBObject();
                        search.put(EQuota._id.name(), oldMk);

                        BasicDBObject update = new BasicDBObject();
                        search.put("$pullAll", new BasicDBObject(EQuota.resources.name(), quotaBasicObject.get(EQuota.resources.name())));
                        db.getCollection(Config.COLLECTION_QUOTA_NAME).update(search, update);
                    }
                }
            } else {
                // exist quota --> update
                BasicDBObject search = new BasicDBObject();
                search.put(EQuota._id.name(), newMk);
                db.getCollection(Config.COLLECTION_QUOTA_NAME).update(search, new BasicDBObject("$push"
                        , new BasicDBObject(EQuota.resources.name()
                        , new BasicDBObject("$each", quotaBasicObject.get(EQuota.resources.name())))));

            }
        }

        if (newQuotaList.size() > 0) {
            this.haveNewQuota = true;
            this.minExpireDate = findMinExpireDate(newQuotaList);
        }
    }


    public void updateTransactionSetQuota(ArrayList<Quota> quotas) {

        //get monitoring key by resource
        Map<String, String> mkMap = new HashMap<>();
        quotas.forEach(quota ->
                quota.getResources().forEach(resourceQuota ->
                        mkMap.put(resourceQuota.getResourceId(), quota.getMonitoringKey())
                )
        );

        List<Transaction> updateTransactionList = new ArrayList<>();
        Collections.copy(updateTransactionList, appInstance.getPcefInstance().getOtherStartTransactions());
        updateTransactionList.add(appInstance.getPcefInstance().getTransaction());

        //update by transaction
        for (Transaction transaction : updateTransactionList) {
            BasicDBObject searchQuery = new BasicDBObject();
            searchQuery.put(ETransaction.tid.name(), transaction.getTid());

            BasicDBObject updateQuery = new BasicDBObject();
            updateQuery.put(ETransaction.monitoringKey.name(), mkMap.get(transaction.getResourceId()));
//            updateQuery.put("monitoringKey", resourceIdMapMonitoringKey.get(resourceId)); //updateDate
//            updateQuery.put("monitoringKey", resourceIdMapMonitoringKey.get(resourceId)); //expireDate +25(config)
            updateQuery.put(ETransaction.status.name(), EStatusLifeCycle.Done.getName());
            updateSetByQuery(Config.COLLECTION_TRANSACTION_NAME, searchQuery, updateQuery);
        }

    }

    public boolean checkQuotaAvailable(Quota quota) {

        BasicDBObject match = new BasicDBObject();
        match.put(ETransaction.monitoringKey.name(), quota.getMonitoringKey());
        match.put(ETransaction.status.name(), EStatusLifeCycle.Done.getName());

        BasicDBObject group = new BasicDBObject();
        group.put("_id", "$" + ETransaction.resourceId.name());
        group.put("count", new BasicDBObject("$sum", 1));

        Iterator<DBObject> groupTransactionByResourceIterator = aggregateMatch(Config.COLLECTION_TRANSACTION_NAME, match, group).iterator();

        int sumTransaction = 0;
        Map<String, String> countUnitMap = new HashMap<>();
        while (groupTransactionByResourceIterator.hasNext()) {
            DBObject dbObject = groupTransactionByResourceIterator.next();

            Integer count = (int) Double.parseDouble(dbObject.get("count").toString());
            String resourceId = dbObject.get("_id").toString();

            if (appInstance.getPcefInstance().getTransaction().getResourceId().equals(resourceId)) {
                count += 1;//resource of this transaction
            }

            sumTransaction += count;

            countUnitMap.put(resourceId, String.valueOf(count));
        }

        int quotaUnit = Integer.parseInt(quota.getQuotaByKey().getUnit());
        if (quotaUnit > sumTransaction) {
            AFLog.d("Quota Available");
            return true;
        } else {
            AFLog.d("Quota Exhaust");
            appInstance.getPcefInstance().setQuotaExhaust(true);
            appInstance.getPcefInstance().setCountUnitMap(countUnitMap);
            appInstance.getPcefInstance().setQuotaCommit(quota);
            return false;
        }
    }


    private void writeQueryLog(String action, String collection, String condition) {
        AFLog.d("[Query] " + action + " " + collection + ", condition =" + condition);
    }


}
