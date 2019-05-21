package phoebe.eqx.pcef.services.mogodb;

import com.google.gson.Gson;
import com.mongodb.*;
import com.mongodb.client.MongoDatabase;
import ec02.af.utils.AFLog;
import phoebe.eqx.pcef.core.data.ResourceResponse;
import phoebe.eqx.pcef.enums.config.EConfig;
import phoebe.eqx.pcef.message.parser.res.OCFUsageMonitoringResponse;
import phoebe.eqx.pcef.core.data.ResourceQuota;
import phoebe.eqx.pcef.enums.EStatusLifeCycle;
import phoebe.eqx.pcef.enums.model.EProfile;
import phoebe.eqx.pcef.enums.model.EQuota;
import phoebe.eqx.pcef.enums.model.ETransaction;
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


    public boolean findMyTransactionDone() {
        Transaction transaction = appInstance.getPcefInstance().getTransaction();

        BasicDBObject searchQuery = new BasicDBObject();
        searchQuery.put(ETransaction.userValue.name(), transaction.getUserValue());
        searchQuery.put(ETransaction.tid.name(), transaction.getTid());
        searchQuery.put(ETransaction.status.name(), EStatusLifeCycle.Done.getName());

        return findByQuery(Config.COLLECTION_TRANSACTION_NAME, searchQuery).hasNext();
    }


    public boolean firstTimeAndWaitProcessing() {
        return true;
    }


    public void filterTransactionConfirmIsNewResource(List<Transaction> otherTransaction) {
        int index = 0;
        for (Transaction transaction : appInstance.getPcefInstance().getOtherStartTransactions()) {
            DBCursor quotaCursor = findQuotaByTransaction(transaction);
            if (quotaCursor.hasNext()) {
                otherTransaction.remove(index);
            }
            index++;
        }
    }


    public List<Transaction> findOtherStartTransaction() {
        try {
            PCEFInstance pcefInstance = appInstance.getPcefInstance();

            BasicDBObject search = new BasicDBObject();
            search.put(ETransaction.status.name(), EStatusLifeCycle.Waiting.getName());
            search.put(ETransaction.userValue.name(), pcefInstance.getUsageMonitoringRequest().getPrivateId());
            search.put(ETransaction.monitoringKey.name(), "");//initial

            ArrayList<String> tidArrayList = new ArrayList<>();
            tidArrayList.add(pcefInstance.getTransaction().getTid());
            search.put(ETransaction.tid.name(), new BasicDBObject("$nin", tidArrayList));


            DBCursor otherTransactionCursor = findByQuery(Config.COLLECTION_TRANSACTION_NAME, search);

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


    public Date findQuotaGetMinimumExpireDate() {
        String privateId = appInstance.getPcefInstance().getTransaction().getUserValue();

        BasicDBObject match = new BasicDBObject();
        match.put(EQuota.userValue.name(), privateId);

        BasicDBObject group = new BasicDBObject();
        group.put("_id", new BasicDBObject());
        group.put("minExpireDate", new BasicDBObject("$min", "$" + EQuota.expireDate.name()));

        //find minimum
        return (Date) aggregateMatch(Config.COLLECTION_QUOTA_NAME, match, group).iterator().next().get("minExpireDate");
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

            int firstNumber = 0;
            profile.setSequenceNumber(firstNumber);
            appInstance.getPcefInstance().setSequenceNumber(firstNumber);

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
            searchQuery.put(EProfile._id.name(), privateId);

            DBCursor dbCursor = findByQuery(Config.COLLECTION_PROFILE_NAME, searchQuery);


            if (dbCursor.hasNext()) {
                DBObject profileDbObject = dbCursor.iterator().next();

                Date appointmentDate = (Date) profileDbObject.get(EProfile.appointmentDate.name());
                String sessionId = (String) profileDbObject.get(EProfile.sessionId.name());
                Integer sequenceNumber = (Integer) profileDbObject.get(EProfile.sequenceNumber.name());

                appInstance.getPcefInstance().setAppointmentDate(appointmentDate);
                appInstance.getPcefInstance().setOcfSessionId(sessionId);
                appInstance.getPcefInstance().setSequenceNumber(sequenceNumber);

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
        updateQuery.put(EProfile.sequenceNumber.name(), appInstance.getPcefInstance().getSequenceNumber());

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
                appInstance.getPcefInstance().setAppointmentDate(minExpireDate);
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

    public DBObject findAndModifyLockQuotaExpire() {
        return findAndModifyLockQuota(appInstance.getPcefInstance().getQuotaExpire().getMonitoringKey());
    }


    //---- Quota function----------------------------------------------------------------------------------------------


    public DBCursor findQuotaByThisTransaction() {
        return findQuotaByTransaction(appInstance.getPcefInstance().getTransaction());
    }


    public DBCursor findQuotaByTransaction(Transaction transaction) {
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


    public ArrayList<Quota> getQuotaFromUsageMonitoringResponse(OCFUsageMonitoringResponse OCFUsageMonitoringResponse) {
        Map<String, Quota> quotaMap = new HashMap<>();
        for (ResourceResponse resourceResponse : OCFUsageMonitoringResponse.getResources()) {
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
                quota.setUserType(OCFUsageMonitoringResponse.getUserType());
                quota.setUserValue(OCFUsageMonitoringResponse.getUserValue());
                quota.setProcessing(0);

                quota.setMonitoringKey(resourceResponse.getMonitoringKey());
                if (resourceResponse.getQuotaByKey() != null) {
                    quota.setCounterId(resourceResponse.getCounterId());
                    quota.setQuotaByKey(resourceResponse.getQuotaByKey());
                    quota.setRateLimitByKey(resourceResponse.getRateLimitByKey());

                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.MINUTE, resourceResponse.getQuotaByKey().getValidityTime());
                    quota.setExpireDate(calendar.getTime());
                }
                quota.getResources().add(resourceQuota);
                quotaMap.put(monitoringKey, quota);
            } else {
                //Same Quota --> update resourceResponse
                myQuota.getResources().add(resourceQuota);
            }
        }
        return new ArrayList<>(quotaMap.values());
    }


    private List<BasicDBObject> getQuotaToBasicObjectList(ArrayList<Quota> quotaResponses) {
        List<BasicDBObject> quotaBasicObjectList = new ArrayList<>();
        for (Quota quota : quotaResponses) {
            BasicDBObject basicDBObject = BasicDBObject.parse(gson.toJson(quota));
            basicDBObject.put(EQuota.expireDate.name(), quota.getExpireDate());
            quotaBasicObjectList.add(basicDBObject);
        }
        return quotaBasicObjectList;
    }


    public void insertQuotaFirstUsage(ArrayList<Quota> quotaResponses) {
        List<BasicDBObject> quotaBasicObjectList = getQuotaToBasicObjectList(quotaResponses);
        insertManyByObject(Config.COLLECTION_QUOTA_NAME, quotaBasicObjectList);
        this.minExpireDate = calMinExpireDate(quotaBasicObjectList);
    }

    public void updateQuota(ArrayList<Quota> quotaResponses) {

        PCEFInstance pcefInstance = appInstance.getPcefInstance();

        List<BasicDBObject> quotaBasicObjectList = getQuotaToBasicObjectList(quotaResponses);
        List<BasicDBObject> newQuotaList = new ArrayList<>();

        //##check mk change by resourceId request
        if (pcefInstance.isQuotaExhaust()) {


            String oldMk = pcefInstance.getQuotaToCommit().getMonitoringKey();
            String newMk = null;

            //find new mk response by resourceId request
            for (Quota quota : quotaResponses) {
                if (newMk != null) {
                    break;
                }
                for (ResourceQuota resourceQuota : quota.getResources()) {
                    if (pcefInstance.getTransaction().getResourceId().equals(resourceQuota.getResourceId())) {
                        newMk = quota.getMonitoringKey();
                        break;
                    }
                }
            }

            if (!oldMk.equals(newMk)) {
                //old quota --> delete
                db.getCollection(Config.COLLECTION_QUOTA_NAME).remove(new BasicDBObject(EQuota._id.name(), oldMk));
            }
        }

        //## update quota
        for (BasicDBObject quotaBasicObject : quotaBasicObjectList) {
            String newMk = quotaBasicObject.get(EQuota.monitoringKey.name()).toString();

            if (quotaBasicObject.get(EQuota.quotaByKey.name()) != null) {
                //new quota --> insert
                insertByQuery(Config.COLLECTION_QUOTA_NAME, quotaBasicObject);
                newQuotaList.add(quotaBasicObject);

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
            minExpireDate = findQuotaGetMinimumExpireDate();
        }

    }
/*
 public void updateQuota(ArrayList<Quota> quotaResponses) {

        List<BasicDBObject> quotaBasicObjectList = getQuotaToBasicObjectList(quotaResponses);
        List<BasicDBObject> newQuotaList = new ArrayList<>();

        for (BasicDBObject quotaBasicObject : quotaBasicObjectList) {
            String newMk = quotaBasicObject.get(EQuota.monitoringKey.name()).toString();

            if (quotaBasicObject.get(EQuota.quotaByKey.name()) != null) {
                //new quota --> insert
                insertByQuery(Config.COLLECTION_QUOTA_NAME, quotaBasicObject);
                newQuotaList.add(quotaBasicObject);

                //remove old quota
                if (appInstance.getPcefInstance().isQuotaExhaust()) {
                    String oldMk = appInstance.getPcefInstance().getQuotaToCommit().getMonitoringKey();

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
            minExpireDate = findQuotaGetMinimumExpireDate();
        }
    }
*/

    public boolean findQuotaExpire() {
        Date currentTime = appInstance.getPcefInstance().getStartTime();

        BasicDBObject search = new BasicDBObject();
        search.put(EQuota.expireDate.name(), new BasicDBObject("$lte", currentTime));
        DBCursor dbCursor = findByQuery(EConfig.COLLECTION_QUOTA_NAME.getConfigName(), search);

        if (dbCursor.hasNext()) {//expire
            DBObject dbObject = dbCursor.next();
            Quota quota = gson.fromJson(gson.toJson(dbObject), Quota.class);
            appInstance.getPcefInstance().setQuotaExpire(quota);

            AFLog.d("expireDate :" + quota.getExpireDate());
            AFLog.d("currentTime = " + currentTime);
            return true;
        } else {
            AFLog.d("quota not found");
        }

        return false;
    }


    public boolean findProfileTimeForAppointmentDate() {


        Date currentTime = appInstance.getPcefInstance().getStartTime();
        BasicDBObject search = new BasicDBObject();
        search.put(EProfile.appointmentDate.name(), new BasicDBObject("$lte", currentTime));

        DBCursor dbCursor = findByQuery(EConfig.COLLECTION_PROFILE_NAME.getConfigName(), search);
        if (dbCursor.hasNext()) {//expire
            DBObject dbObject = dbCursor.next();
            Profile profile = gson.fromJson(gson.toJson(dbObject), Profile.class);
            appInstance.getPcefInstance().setProfile(profile);

            AFLog.d("userValue :" + profile.getUserValue());
            AFLog.d("appointmentDate :" + profile.getAppointmentDate());
            AFLog.d("currentTime = :" + currentTime);

            return true;
        } else {
            AFLog.d("profile not found");
        }

        return false;
    }

    public Date calMinExpireDate(List<BasicDBObject> quotaBasicObjectList) {
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


    public void updateTransaction(ArrayList<Quota> quotas) {

        //get monitoring key by resource
        Map<String, String> mkMap = new HashMap<>();
        quotas.forEach(quota ->
                quota.getResources().forEach(resourceQuota ->
                        mkMap.put(resourceQuota.getResourceId(), quota.getMonitoringKey())
                )
        );

        List<Transaction> updateTransactionList = new ArrayList<>();

        //update transaction Done to Complete
        if (appInstance.getPcefInstance().isQuotaExhaust()) {
            BasicDBObject searchQuery = new BasicDBObject();

            searchQuery.put(ETransaction.monitoringKey.name(), appInstance.getPcefInstance().getQuotaToCommit().getMonitoringKey());
            searchQuery.put(ETransaction.userValue.name(), appInstance.getPcefInstance().getQuotaToCommit().getUserValue());
            searchQuery.put(ETransaction.status.name(), EStatusLifeCycle.Done.getName());

            BasicDBObject updateQuery = new BasicDBObject();
            updateQuery.put(ETransaction.updateDate.name(), new Date()); //updateDate
            updateQuery.put(ETransaction.status.name(), EStatusLifeCycle.Completed.getName());

            db.getCollection(Config.COLLECTION_TRANSACTION_NAME).update(searchQuery, new BasicDBObject("$set", updateQuery), false, true);
        }


        updateTransactionList.add(appInstance.getPcefInstance().getTransaction());
        updateTransactionList.addAll(appInstance.getPcefInstance().getOtherStartTransactions());

        //update by transaction
        for (Transaction transaction : updateTransactionList) {
            BasicDBObject searchQuery = new BasicDBObject();

            searchQuery.put(ETransaction.tid.name(), transaction.getTid());
            searchQuery.put(ETransaction.status.name(), EStatusLifeCycle.Waiting.getName());

            BasicDBObject updateQuery = new BasicDBObject();
            if (appInstance.getPcefInstance().isQuotaExhaust() && appInstance.getPcefInstance().getTransaction().getTid().equals(transaction.getTid())) {
                updateQuery.put(ETransaction.status.name(), EStatusLifeCycle.Completed.getName());//waiting -->Complete
            } else {
                updateQuery.put(ETransaction.status.name(), EStatusLifeCycle.Done.getName());//waiting-->Done
            }
            updateQuery.put(ETransaction.monitoringKey.name(), mkMap.get(transaction.getResourceId()));
            updateQuery.put(ETransaction.updateDate.name(), new Date());

            updateSetByQuery(Config.COLLECTION_TRANSACTION_NAME, searchQuery, updateQuery);
        }


    }

    public Map<String, Integer> findTransactionDoneGroupByResourceQuotaExpire() {
        return findTransactionDoneGroupByResource(appInstance.getPcefInstance().getQuotaExpire());
    }


    public Map<String, Integer> findTransactionDoneGroupByResource(Quota quota) {
        BasicDBObject match = new BasicDBObject();
        match.put(ETransaction.monitoringKey.name(), quota.getMonitoringKey());
        match.put(ETransaction.status.name(), EStatusLifeCycle.Done.getName());

        BasicDBObject group = new BasicDBObject();
        group.put("_id", "$" + ETransaction.resourceId.name());
        group.put("count", new BasicDBObject("$sum", 1));

        Map<String, Integer> countUnitByResourceMap = new HashMap<>();
        Iterator<DBObject> transactionGroupByResourceIterator = aggregateMatch(Config.COLLECTION_TRANSACTION_NAME, match, group).iterator();
        while (transactionGroupByResourceIterator.hasNext()) {
            DBObject dbObject = transactionGroupByResourceIterator.next();

            int count = (int) Double.parseDouble(dbObject.get("count").toString());
            String resourceId = dbObject.get("_id").toString();

            if (appInstance.getPcefInstance().getTransaction().getResourceId().equals(resourceId)) {
                count += 1;//resource of this transaction
            }

            countUnitByResourceMap.put(resourceId, count);
        }
        appInstance.getPcefInstance().setCountUnitMap(countUnitByResourceMap);

        return countUnitByResourceMap;
    }

    public boolean checkQuotaAvailable(Quota quota) {

        Map<String, Integer> countUnitByResourceMap = findTransactionDoneGroupByResource(quota);
        int sumTransaction = countUnitByResourceMap.values().stream().mapToInt(count -> count).sum();

        int quotaUnit = quota.getQuotaByKey().getUnit();
        if (quotaUnit > sumTransaction) {
            AFLog.d("Quota Available");
            return true;
        } else {
            AFLog.d("Quota Exhaust");
            appInstance.getPcefInstance().setQuotaExhaust(true);
            appInstance.getPcefInstance().setQuotaForCommit(quota);
            return false;
        }
    }

    private void writeQueryLog(String action, String collection, String condition) {
        AFLog.d("[Query] " + action + " " + collection + ", condition =" + condition);
    }


}
