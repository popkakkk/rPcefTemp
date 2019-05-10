package phoebe.eqx.pcef.services;

import com.google.gson.Gson;
import com.mongodb.*;
import ec02.af.utils.AFLog;
import phoebe.eqx.pcef.core.data.Resource;
import phoebe.eqx.pcef.enums.EStatusLifeCycle;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.instance.Config;
import phoebe.eqx.pcef.instance.PCEFInstance;
import phoebe.eqx.pcef.message.parser.req.UsageMonitoringRequest;
import phoebe.eqx.pcef.model.Profile;
import phoebe.eqx.pcef.model.Quota;
import phoebe.eqx.pcef.model.Transaction;
import phoebe.eqx.pcef.utils.MessageFlow;
import phoebe.eqx.pcef.utils.PCEFUtils;

import java.util.*;

public class MongoDBService {

    private AppInstance appInstance;
    private MongoClient mongoClient;
    private DB db;
    private Gson gson = new Gson();

    //---- Connect DB-----------------------------------------------------
    public MongoDBService(AppInstance appInstance) {
        this.appInstance = appInstance;
        this.connectDB(Config.MONGODB_URL, Config.MY_DB_NAME);
    }

    private void connectDB(String url, String dbName) {
        mongoClient = new MongoClient(new MongoClientURI(url));
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

    private DBCursor findByObject(String collectionName, Object object) {
        String json = gson.toJson(object);
        writeQueryLog("find", collectionName, json);
        return db.getCollection(collectionName).find(BasicDBObject.parse(json));
    }

    private DBCursor findByQuery(String collectionName, BasicDBObject whereQuery) {
        writeQueryLog("find", collectionName, whereQuery.toJson());
        return db.getCollection(collectionName).find(whereQuery);
    }

    private void updateByQuery(String collectionName, BasicDBObject searchQuery, BasicDBObject updateQuery) {
        String condition = "searchQuery:" + searchQuery.toJson() + ",updateQuery:" + updateQuery.toJson();
        writeQueryLog("update", collectionName, condition);
        db.getCollection(collectionName).update(searchQuery, updateQuery);
    }


    public Iterable<DBObject> aggregateMatch(String collectionName, BasicDBObject match, BasicDBObject group) {
        List<DBObject> pipeline = Arrays.asList(
                new BasicDBObject("$match", match)
                , new BasicDBObject("$group", new BasicDBObject("_id", group)));

        String condition = "$match:" + match.toJson() + ",$group:" + group.toJson();
        writeQueryLog("aggregate", collectionName, condition);
        return db.getCollection(collectionName).aggregate(pipeline).results();
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
            transaction.setUserType("privateId");//!
            transaction.setUserValue(usageMonitoringRequest.getPrivateId());
            transaction.setResourceId(resourceId);
            transaction.setResourceName(usageMonitoringRequest.getResourceName());
            transaction.setMonitoringKey("");// "" is initial
//        transaction.setCounterId();

//            String dateNow = PCEFUtils.transactionDateFormat.format(new Date());
            Date now = new Date();
            transaction.setCreateDate(now);
            transaction.setUpdateDate(now);
            transaction.setStatus(EStatusLifeCycle.Waiting.getName());
//        transaction.setApp();
            transaction.setClientId(usageMonitoringRequest.getClientId());

            transaction.setFirstTime("1");
            transaction.setIsActive("1");
            insertByObject(Config.COLLECTION_TRANSACTION_NAME, transaction);
            appInstance.getPcefInstance().setTransaction(transaction);
            PCEFUtils.writeMessageFlow("Insert Transaction", MessageFlow.Status.Success, appInstance.getPcefInstance().getSessionId());
        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Insert Transaction", MessageFlow.Status.Error, appInstance.getPcefInstance().getSessionId());

        }
    }

    public boolean isNotThisTransaction() {

        return true;
    }


    public DBCursor findTransactionByStatus(String status) {
        String privateId = appInstance.getPcefInstance().getUsageMonitoringRequest().getPrivateId();
        DBCollection collection = db.getCollection(Config.COLLECTION_TRANSACTION_NAME);
        BasicDBObject searchQuery = new BasicDBObject();
        searchQuery.put("privateId", privateId);
        return collection.find(searchQuery);
    }


    public boolean firstTimeAndWaitProcessing() {
        return true;
    }


    public List<Resource> findTransactionWaitingOfProfile() {
        try {

            PCEFInstance pcefInstance = appInstance.getPcefInstance();

            BasicDBObject match = new BasicDBObject();
            match.put("status", EStatusLifeCycle.Waiting.getName());
            match.put("userValue", pcefInstance.getUsageMonitoringRequest().getPrivateId());
            match.put("monitoringKey", "");//initial

            List<String> resourceIdThisTransaction = new ArrayList<>();
            resourceIdThisTransaction.add(pcefInstance.getTransaction().getResourceId());
            match.put("resourceId", new BasicDBObject("$nin", resourceIdThisTransaction));//resourceId must != this.transaction.resourceId

            BasicDBObject group = new BasicDBObject();
            group.put("resourceId", "$resourceId");
            group.put("resourceName", "$resourceName");


            List<Resource> resources = new ArrayList<>();
            Iterator<DBObject> iterator = aggregateMatch(Config.COLLECTION_TRANSACTION_NAME, match, group).iterator();
            while (iterator.hasNext()) {
                DBObject dbObject = iterator.next();
                DBObject result = (DBObject) dbObject.get("_id");

                Resource resource = new Resource();
                resource.setResourceId(result.get("resourceId").toString());
                resource.setResourceName(result.get("resourceName").toString());
                resources.add(resource);
            }

            return resources;
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
//            profile.setUserType();
//            profile.setUserValue();
            profile.setIsProcessing(1);
            profile.setSequenceNumber(1);
            profile.setSessionId(appInstance.getPcefInstance().getUsageMonitoringRequest().getSessionId());

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


    public boolean checkProfileIsProcessing(DBCursor lockProcessCursor) {
        DBObject dbObject = lockProcessCursor.next();
        String isProcessing = String.valueOf(dbObject.get("isProcessing"));
        return isProcessing.equals("0");
    }


    public DBCursor findProfileByPrivateId() {

        try {
            String privateId = appInstance.getPcefInstance().getUsageMonitoringRequest().getPrivateId();
            BasicDBObject searchQuery = new BasicDBObject();
            searchQuery.put("privateId", privateId);

            DBCursor dbCursor = findByQuery(Config.COLLECTION_PROFILE_NAME, searchQuery);

            //message flow
            if (dbCursor.hasNext()) {
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

    public void updateProcessingLockProcess() {

        String privateId = "";

        DBCollection collection = db.getCollection(Config.COLLECTION_PROFILE_NAME);

        BasicDBObject searchQuery = new BasicDBObject();
        searchQuery.put("privateId", privateId);

        BasicDBObject updateQuery = new BasicDBObject();
        updateQuery.put("isProcessing", "0");


        collection.update(searchQuery, updateQuery);
    }


    //---- Quota function----------------------------------------------------------------------------------------------

    public DBCursor findMonitoringKey() {
        String privateId = "";
        String resourceName = "";

        DBCollection collection = db.getCollection(Config.COLLECTION_TRANSACTION_NAME);
        BasicDBObject searchQuery = new BasicDBObject();
        searchQuery.put("privateId", privateId);
        searchQuery.put("resourceName", resourceName);
        return collection.find(searchQuery);
    }

    public boolean checkMkIsProcessing(DBCursor monitoringKeyCursor) {
        DBObject dbObject = monitoringKeyCursor.next();
        String isProcessing = String.valueOf(dbObject.get("isProcessing"));
        return isProcessing.equals("0");
    }


    public DBCursor findMonitroingKey() {
        String privateId = "";
        String resourceName = "";

        DBCollection collection = db.getCollection(Config.COLLECTION_TRANSACTION_NAME);
        BasicDBObject searchQuery = new BasicDBObject();
        searchQuery.put("privateId", privateId);
        searchQuery.put("resourceName", resourceName);
        return collection.find(searchQuery);
    }


    public void insertQuota() {

        Quota quota = new Quota();
       /* quota.set_id();
        quota.setUserType();
        quota.setUserValue();
        quota.setMainProcessing();
        quota.setExpirtDate();
        quota.setMonitoringKey();
        quota.setCounterId();
        quota.setQuota();
        quota.setVt();
        quota.setTransactionPerTime();
        quota.setResource();
*/

        insertByObject(Config.COLLECTION_QUOTA_NAME, quota);
    }


    public void updateMonitoringKeyTransaction() {

    }



   /* public boolean checkIsUsageMonitoringStartState() {

        boolean isStartState = false;
        boolean check = false;

        //set interval obj
        Interval intervalIsProcessing = new Interval(Config.RETRY_PROCESSING, Config.INTERVAL_PROCESSING);
        Interval intervalInsertLockProcess = new Interval(Config.RETRY_PROCESSING, Config.INTERVAL_PROCESSING);

        //check is START state
        while (!check) {
            DBCursor lockProcessCursor = findProfileByPrivateId();

            //not found
            if (!lockProcessCursor.hasNext()) {
                try {
                    *//*Insert Profile*//*
                    boolean insertByObject = insertProfile();
                    if (!insertByObject) {
                        intervalInsertLockProcess.waitIntervalAndRetry(() -> insertProfile());
                    }
                    isStartState = true;
                } catch (TimeoutIntervalException e) {
                    //kill
                } catch (DuplicateKeyException e) {
                    continue;
                }
            }
            //#found and isProcessing = 0
            else if (checkProfileIsProcessing(lockProcessCursor)) {
                try {
                    intervalIsProcessing.waitInterval();
                    continue;
                } catch (TimeoutIntervalException e) {
                    isStartState = true;
                }
            }
            check = true;
        }
        return isStartState;
    }*/


    private void writeQueryLog(String action, String collection, String condition) {
        AFLog.d("[Query] " + action + " " + collection + ", condition =" + collection);
    }


}
