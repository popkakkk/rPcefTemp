package phoebe.eqx.pcef.services.mogodb;

import com.google.gson.Gson;
import com.mongodb.*;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Updates;
import ec02.af.utils.AFLog;
import phoebe.eqx.pcef.core.data.OCFUsageMonitoring;
import phoebe.eqx.pcef.core.data.Resource;
import phoebe.eqx.pcef.core.data.ResourceQuota;
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

import java.text.ParseException;
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
                , new BasicDBObject("$group", new BasicDBObject("_id", group)));

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
            transaction.setFirstTime(1);
            transaction.setIsActive(1);


            insertByObject(Config.COLLECTION_TRANSACTION_NAME, transaction);


            List<Transaction> transactions = new ArrayList<>();
            transactions.add(transaction);
            appInstance.getPcefInstance().setTransactions(transactions);
            appInstance.getPcefInstance().setTid(transaction.getTid());
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


    public void findOtherStartTransaction() {
        try {

            PCEFInstance pcefInstance = appInstance.getPcefInstance();

            BasicDBObject match = new BasicDBObject();
            match.put("status", EStatusLifeCycle.Waiting.getName());
            match.put("userValue", pcefInstance.getUsageMonitoringRequest().getPrivateId());
            match.put("monitoringKey", "");//initial

            List<String> resourceIdThisTransaction = new ArrayList<>();
            resourceIdThisTransaction.add(pcefInstance.getTransactions().get(0).getResourceId());
            match.put("resourceId", new BasicDBObject("$nin", resourceIdThisTransaction));//resourceId must != this.transaction.resourceId

            BasicDBObject group = new BasicDBObject();
            group.put("resourceId", "$resourceId");
            group.put("resourceName", "$resourceName");
            group.put("rtid", "$rtid");
            group.put("tid", "$tid");

            Iterator<DBObject> iterator = aggregateMatch(Config.COLLECTION_TRANSACTION_NAME, match, group).iterator();

            List<Transaction> otherStartTransactionList = new ArrayList<>();
            while (iterator.hasNext()) {
                DBObject dbObject = iterator.next();
                DBObject result = (DBObject) dbObject.get("_id");

                Transaction transaction = new Transaction();
                transaction.setResourceId(result.get("resourceId").toString());
                transaction.setResourceName(result.get("resourceName").toString());
                transaction.setTid(result.get("tid").toString());
                transaction.setRtid(result.get("rtid").toString());
                otherStartTransactionList.add(transaction);
            }
            pcefInstance.getTransactions().addAll(otherStartTransactionList);

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
            profile.setSessionId(appInstance.getPcefInstance().getUsageMonitoringRequest().getSessionId());


//            profile.setAppointmentDate(PCEFUtils.isoDateFormatter.format(new Date()));

           /*
            BasicDBObject profileDBObject = BasicDBObject.parse(gson.toJson(profile));
            profileDBObject.put("date", new Date());
            insertByQuery(Config.COLLECTION_PROFILE_NAME, profileDBObject);
*/

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
        String isProcessing = String.valueOf(dbObject.get("isProcessing"));
        return isProcessing.equals("0");
    }


    public DBCursor findProfileByPrivateId() throws ParseException {

        try {
            String privateId = appInstance.getPcefInstance().getUsageMonitoringRequest().getPrivateId();
            BasicDBObject searchQuery = new BasicDBObject();
            searchQuery.put("userValue", privateId);

            DBCursor dbCursor = findByQuery(Config.COLLECTION_PROFILE_NAME, searchQuery);

            //message flow
            if (dbCursor.hasNext()) {
                Date appointmentDate = (Date) dbCursor.iterator().next().get("appointmentDate");
                appInstance.getPcefInstance().setAppointmentDate(appointmentDate);

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
        BasicDBObject searchQuery = new BasicDBObject();
        searchQuery.put("userValue", appInstance.getPcefInstance().getTransactions().get(0).getUserValue());
        searchQuery.put("isProcessing", 1);

        updateQuery.put("isProcessing", 0);
        updateSetByQuery(Config.COLLECTION_PROFILE_NAME, searchQuery, updateQuery);
    }


    public void updateProfileUnLockInitial() {
        BasicDBObject updateQuery = new BasicDBObject();
        updateQuery.put("appointmentDate", minExpireDate);
        updateProfileUnlock(updateQuery);
    }

    public void updateProfileUnLock() {
        BasicDBObject updateQuery = new BasicDBObject();
        if (haveNewQuota) {
            if (minExpireDate.before(appInstance.getPcefInstance().getAppointmentDate())) {
                updateQuery.put("appointmentDate", minExpireDate);
            }
        }
        updateProfileUnlock(updateQuery);
    }

    public DBObject findAndModifyProfile() {

        String privateId = appInstance.getPcefInstance().getTransactions().get(0).getUserValue();
        BasicDBObject query = new BasicDBObject();
        query.put("userValue", privateId);
        query.put("isProcessing", 0);

        BasicDBObject update = new BasicDBObject();
        update.put("isProcessing", 1);

        return findAndModify(Config.COLLECTION_PROFILE_NAME, query, update);
    }


  /*  public void findAndModifyProfile(boolean processing) {
        try {
            String privateId = appInstance.getPcefInstance().getTransactions().get(0).getUserValue();
            BasicDBObject searchQuery = new BasicDBObject();
            searchQuery.put("privateId", privateId);

            BasicDBObject updateQuery = new BasicDBObject();
            if (processing) {//lock
                searchQuery.put("isProcessing", "0");
                updateQuery.put("isProcessing", "1");
            } else {//unlock
                updateQuery.put("isProcessing", "0");
            }
            updateSetByQuery(Config.COLLECTION_PROFILE_NAME, searchQuery, updateQuery);
        } catch (Exception e) {
            AFLog.d("find and modify profile error -" + e.getMessage());

        }
    }*/


    //---- Quota function----------------------------------------------------------------------------------------------

    public DBCursor findQuotaByResource() {
        Transaction transaction = appInstance.getPcefInstance().getTransactions().get(0);
        BasicDBObject searchQuery = new BasicDBObject();
        searchQuery.put("userValue", transaction.getUserValue());
        searchQuery.put("resources", new BasicDBObject("$elemMatch", new BasicDBObject("resourceId", transaction.getResourceId())));
        return findByQuery(Config.COLLECTION_QUOTA_NAME, searchQuery);
    }

    public boolean checkMkCanProcess(DBCursor monitoringKeyCursor) {
        DBObject dbObject = monitoringKeyCursor.iterator().next();
        String isProcessing = String.valueOf(dbObject.get("mainProcessing"));
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

    public ArrayList<Quota> getQuotaFromUsageMonitoringResponse(OCFUsageMonitoring usageMonitoringResponse) {
        Transaction transaction = appInstance.getPcefInstance().getTransactions().get(0);
        Map<String, Quota> quotaMap = new HashMap<>();
        for (Resource resource : usageMonitoringResponse.getResources()) {
            String monitoringKey = resource.getMonitoringKey();
            String resourceName = resource.getResourceName();
            String resourceId = resource.getResourceId();

            ResourceQuota resourceQuota = new ResourceQuota();
            resourceQuota.setResourceId(resourceId);
            resourceQuota.setResourceName(resourceName);

            Quota myQuota = quotaMap.get(monitoringKey);
            if (myQuota == null) {
                Quota quota = new Quota();
                quota.set_id(resource.getMonitoringKey());
                quota.setUserType(transaction.getUserType());
                quota.setUserValue(transaction.getUserValue());
                quota.setMainProcessing("0");
//                quota.setExpireDate();
                quota.setMonitoringKey(resource.getMonitoringKey());
                quota.setCounterId(resource.getCounterId());
                quota.setQuotaByKey(resource.getQuotaByKey());
                quota.setRateLimitByKey(resource.getRateLimitByKey());
                quota.getResources().add(resourceQuota);
                quotaMap.put(monitoringKey, quota);
            } else {//Same Quota --> update resource
                myQuota.getResources().add(resourceQuota);
            }
        }
        return new ArrayList<>(quotaMap.values());
    }


    private List<BasicDBObject> setExpireDateISO(ArrayList<Quota> quotaArrayList) {
        List<BasicDBObject> quotaBasicObjectList = new ArrayList<>();
        for (Quota quota : quotaArrayList) {
            BasicDBObject basicDBObject = BasicDBObject.parse(gson.toJson(quota));

            if (quota.getQuotaByKey() != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.MINUTE, Integer.parseInt(quota.getQuotaByKey().getValidityTime()));
                basicDBObject.put("expireDate", calendar.getTime());
            }

            quotaBasicObjectList.add(basicDBObject);
        }
        return quotaBasicObjectList;
    }


    public void insertQuotaStartFirstUsage(ArrayList<Quota> quotaArrayList) {
        List<BasicDBObject> quotaBasicObjectList = setExpireDateISO(quotaArrayList);
        insertManyByObject(Config.COLLECTION_QUOTA_NAME, quotaBasicObjectList);
        this.minExpireDate = findMinExpireDate(quotaBasicObjectList);
    }

    public Date findMinExpireDate(List<BasicDBObject> quotaBasicObjectList) {
        Date minDate = null;
        for (BasicDBObject basicDBObject : quotaBasicObjectList) {
            Date date = (Date) basicDBObject.get("expireDate");
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


    public void updateQuota(ArrayList<Quota> quotaArrayList) {

        List<BasicDBObject> quotaBasicObjectList = setExpireDateISO(quotaArrayList);
        List<BasicDBObject> newQuotaList = new ArrayList<>();

        for (BasicDBObject quotaDBObject : quotaBasicObjectList) {


            if (quotaDBObject.get("quotaByKey") != null) {  //new quota --> insert
                insertByQuery(Config.COLLECTION_QUOTA_NAME, quotaDBObject);
                newQuotaList.add(quotaDBObject);
            } else {// old quota --> update
                BasicDBObject search = new BasicDBObject();
                search.put("_id", quotaDBObject.get("monitoringKey"));
                db.getCollection(Config.COLLECTION_QUOTA_NAME).update(search, new BasicDBObject("$push", new BasicDBObject("resources", new BasicDBObject("$each", quotaDBObject.get("resources")))));
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

        //update by transaction
        for (Transaction transaction : appInstance.getPcefInstance().getTransactions()) {
            BasicDBObject searchQuery = new BasicDBObject();
            searchQuery.put("tid", transaction.getTid());

            BasicDBObject updateQuery = new BasicDBObject();
            updateQuery.put("monitoringKey", mkMap.get(transaction.getResourceId()));
//            updateQuery.put("monitoringKey", resourceIdMapMonitoringKey.get(resourceId)); //updateDate
//            updateQuery.put("monitoringKey", resourceIdMapMonitoringKey.get(resourceId)); //expireDate +25(config)
            updateQuery.put("status", EStatusLifeCycle.Done.getName());
            updateSetByQuery(Config.COLLECTION_TRANSACTION_NAME, searchQuery, updateQuery);
        }

    }

    public boolean checkQuotaAvailable(Quota quota) {
        BasicDBObject query = new BasicDBObject();
        query.put("monitoringKey", quota.getMonitoringKey());
        query.put("status", EStatusLifeCycle.Done.getName());

        int transactionUnit = findByQuery(Config.COLLECTION_TRANSACTION_NAME, query).count();
        int quotaUnit = Integer.parseInt(quota.getQuotaByKey().getUnit());

        if (transactionUnit >= quotaUnit) {
            AFLog.d("Quota Exhaust");
            return true;
        } else {
            AFLog.d("Quota Available");
            return false;
        }
    }


    private void writeQueryLog(String action, String collection, String condition) {
        AFLog.d("[Query] " + action + " " + collection + ", condition =" + condition);
    }


}
