package phoebe.eqx.pcef.services;

import com.google.gson.Gson;
import com.mongodb.*;
import phoebe.eqx.pcef.core.exceptions.TimeoutIntervalException;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.instance.Config;
import phoebe.eqx.pcef.message.Metering;
import phoebe.eqx.pcef.model.LockProcess;
import phoebe.eqx.pcef.model.Quota;
import phoebe.eqx.pcef.model.Transaction;
import phoebe.eqx.pcef.utils.Interval;

public class MongoDBService {


    private AppInstance appInstance;
    MongoClient mongoClient;
    private DB db;

    public MongoDBService(AppInstance appInstance) {
        this.appInstance = appInstance;
        this.connectDB(Config.MONGODB_URL, Config.MY_DB_NAME);
    }

    private void connectDB(String url, String dbName) {
        mongoClient = new MongoClient(new MongoClientURI(url));
        this.db = mongoClient.getDB(dbName);
    }


    public void insertTransaction() {
        try {
            Metering metering = appInstance.getPcefInstance().getMetering();
            String resourceId = appInstance.getPcefInstance().getResource_id_test();

            Transaction transaction = new Transaction();
            transaction.setSessionId(metering.getSessionId());
            transaction.setRtid(metering.getRtid());
            transaction.setTid(metering.getTid());
            transaction.setActualTime(metering.getActualTime());
//        transaction.setUserType();
            transaction.setResourceId(resourceId);
            transaction.setResourceName(metering.getResourceName());
//        transaction.setMonitoringKey();
//        transaction.setCounterId();
//        transaction.setCreateDate();
            transaction.setStatus(Transaction.EStatus.Waiting.getName());
//        transaction.setApp();
            transaction.setClientId(metering.getClientId());

            transaction.setFirstTime("1");
            transaction.setIsActive("1");
            insert(Config.COLLECTION_TRANSACTION_NAME, transaction);
        } catch (Exception e) {


        }
    }


    private void insert(String collectionName, Object object) {
        String json = new Gson().toJson(object);
        BasicDBObject doc = BasicDBObject.parse(json);
        DBCollection collection = db.getCollection(collectionName);
        collection.insert(doc);
    }


    public boolean insertLockProcess() {
        try {
            Metering metering = appInstance.getPcefInstance().getMetering();
            LockProcess lockProcess = new LockProcess();
            lockProcess.set_id(metering.getPrivateId());
            lockProcess.setPrivateId(metering.getPrivateId());
            lockProcess.setIsProcessing(1); // #1 = processing
            lockProcess.setSequenceNumber(1);
            lockProcess.setSessionId(appInstance.getPcefInstance().getMetering().getSessionId());
            insert(Config.COLLECTION_LOCK_PROCESS_NAME, lockProcess);
            return true;
        } catch (DuplicateKeyException e) {
            throw e;
        } catch (Exception e) {
            return false;
        }
    }


    public boolean isNotThisTransaction() {


        return true;
    }


    public DBCursor findTransactionByStatus(String status) {
        String privateId = appInstance.getPcefInstance().getMetering().getPrivateId();
        DBCollection collection = db.getCollection(Config.COLLECTION_TRANSACTION_NAME);
        BasicDBObject searchQuery = new BasicDBObject();
        searchQuery.put("privateId", privateId);
        return collection.find(searchQuery);
    }


    public boolean firstTimeAndWaitProcessing() {
        return true;
    }


    private DBCursor find(String collection, BasicDBObject searchQuery) {
        return db.getCollection(collection).find(searchQuery);
    }


    public DBCursor findTransactionForFirstUsage() {
        Transaction transactionQuery = new Transaction();
        transactionQuery.setIsActive("1");
        transactionQuery.setStatus(Transaction.EStatus.Waiting.getName());
        transactionQuery.setMonitoringKey("");

        return find(Config.COLLECTION_TRANSACTION_NAME, BasicDBObject.parse(new Gson().toJson(transactionQuery)));


    }

    public void updateTransactionFirstTime() {

    }

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


    public boolean checkProfileIsProcessing(DBCursor lockProcessCursor) {
        DBObject dbObject = lockProcessCursor.next();
        String isProcessing = String.valueOf(dbObject.get("isProcessing"));
        return isProcessing.equals("0");
    }


    public DBCursor findProfileByPrivateId() {
        String privateId = appInstance.getPcefInstance().getMetering().getPrivateId();
        DBCollection collection = db.getCollection(Config.COLLECTION_LOCK_PROCESS_NAME);
        BasicDBObject searchQuery = new BasicDBObject();
        searchQuery.put("privateId", privateId);
        return collection.find(searchQuery);
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
        quota.setAppointmentDate();
        quota.setMonitoringKey();
        quota.setCounterId();
        quota.setQuota();
        quota.setVt();
        quota.setTransactionPerTime();
        quota.setResource();
*/

        insert(Config.COLLECTION_QUOTA_NAME, quota);
    }


    public void updateProcessingLockProcess() {

        String privateId = "";

        DBCollection collection = db.getCollection(Config.COLLECTION_LOCK_PROCESS_NAME);

        BasicDBObject searchQuery = new BasicDBObject();
        searchQuery.put("privateId", privateId);

        BasicDBObject updateQuery = new BasicDBObject();
        updateQuery.put("isProcessing", "0");


        collection.update(searchQuery, updateQuery);


    }


    public boolean checkIsUsageMonitoringStartState() {

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
                    /*Insert LockProcess*/
                    boolean insert = insertLockProcess();
                    if (!insert) {
                        intervalInsertLockProcess.waitIntervalAndRetry(() -> insertLockProcess());
                    }
                    isStartState = true;
                } catch (TimeoutIntervalException e) {
                    //kill
                } catch (DuplicateKeyException e) {
                    continue;
                }
            }
            //#found and isPr0cessing = 0
            else if (!checkProfileIsProcessing(lockProcessCursor)) {
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
    }


    public void closeConnection() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    public void updateMonitoringKeyTransaction() {

    }

}
