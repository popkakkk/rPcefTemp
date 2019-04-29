package phoebe.eqx.pcef.services;

import com.google.gson.Gson;
import com.mongodb.*;
import ec02.af.utils.AFLog;
import phoebe.eqx.pcef.enums.EState;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.instance.Config;
import phoebe.eqx.pcef.message.req.MeteringRequest;
import phoebe.eqx.pcef.model.LockProcess;
import phoebe.eqx.pcef.model.Quota;
import phoebe.eqx.pcef.model.Transaction;

public class MongoDBService {

    private static final String TRANSACTION_COLLECTION_NAME = "Transaction";
    private static final String QUOTA_COLLECTION_NAME = "Quota";
    private static final String LOCK_PROCESS_COLLECTION_NAME = "LockProcess";

    private AppInstance appInstance;
    private DB db;

    public MongoDBService(AppInstance appInstance) {
        this.appInstance = appInstance;
        this.connectDB(Config.MONGODB_URL, Config.MY_DB_NAME);
    }

    private void connectDB(String url, String dbName) {
        try {
            MongoClient mongoClient = new MongoClient(new MongoClientURI(url));
            this.db = mongoClient.getDB(dbName);
        } catch (Exception e) {
            AFLog.e("Connect MongoDB failed!", e);
        }
    }


    public void insertTransaction(String resourceId) {
        MeteringRequest meteringRequest = appInstance.getPcefInstance().getMeteringRequest();

        Transaction transaction = new Transaction();
        transaction.setSessionId(meteringRequest.getSessionId());
        transaction.setRtid(meteringRequest.getRtid());
        transaction.setTid(meteringRequest.getTid());
        transaction.setActualTime(meteringRequest.getActualTime());
//        transaction.setUserType();
        transaction.setResourceId(resourceId);
        transaction.setResourceName(meteringRequest.getResourceName());
//        transaction.setMonitoringKey();
//        transaction.setCounterId();
//        transaction.setCreateDate();
        transaction.setStatus(Transaction.EStatus.Waiting.getName());
//        transaction.setApp();
        transaction.setClientId(meteringRequest.getClientId());
        insert(TRANSACTION_COLLECTION_NAME, transaction);
    }


    private void insert(String collectionName, Object object) {
        String json = new Gson().toJson(object);
        BasicDBObject doc = BasicDBObject.parse(json);
        DBCollection collection = db.getCollection(collectionName);
        collection.insert(doc);

    }


    public void insertLockProcess() {
        MeteringRequest meteringRequest = appInstance.getPcefInstance().getMeteringRequest();
        LockProcess lockProcess = new LockProcess();
        lockProcess.set_id(meteringRequest.getPrivateId());
        lockProcess.setPrivateId(meteringRequest.getPrivateId());
        lockProcess.setIsProcessing(1); // #1 = processing
        lockProcess.setSequenceNumber(1);
        lockProcess.setSessionId(appInstance.getPcefInstance().getMeteringRequest().getSessionId());
        insert(LOCK_PROCESS_COLLECTION_NAME, lockProcess);
    }


    public void waitIntervalIsProcessing() throws Exception {
        boolean canProcess = false;
        int retry = 0;
        while (!canProcess) {
            retry++;
            if (retry > Config.RETRY_PROCESSING) {
                throw new Exception("can not retry processing");
            }
            Thread.sleep(Config.INTERVAL_PROCESSING * 1000);
            canProcess = checkCanProcess(findLockProcess());
        }
    }


    public boolean checkCanProcess(DBCursor lockProcessCursor) {
        DBObject dbObject = lockProcessCursor.next();
        String isProcessing = String.valueOf(dbObject.get("isProcessing"));
        return isProcessing.equals("0");
    }


    public DBCursor findLockProcess() {
        String privateId = appInstance.getPcefInstance().getMeteringRequest().getPrivateId();


        DBCollection collection = db.getCollection(LOCK_PROCESS_COLLECTION_NAME);
        BasicDBObject searchQuery = new BasicDBObject();
        searchQuery.put("privateId", privateId);
        return collection.find(searchQuery);
    }


    public DBCursor findMonitroingKey() {
        String privateId = "";
        String resourceName = "";

        DBCollection collection = db.getCollection(TRANSACTION_COLLECTION_NAME);
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

        insert(QUOTA_COLLECTION_NAME, quota);
    }


    public void updateProcessingLockProcess() {

        String privateId = "";

        DBCollection collection = db.getCollection(LOCK_PROCESS_COLLECTION_NAME);

        BasicDBObject searchQuery = new BasicDBObject();
        searchQuery.put("privateId", privateId);

        BasicDBObject updateQuery = new BasicDBObject();
        updateQuery.put("isProcessing", "0");


        collection.update(searchQuery, updateQuery);


    }


    public void updateMonitoringKeyTransaction() {

    }

}
