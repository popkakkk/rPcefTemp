package phoebe.eqx.pcef.services.mogodb;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import ec02.af.utils.AFLog;
import phoebe.eqx.pcef.core.data.ResourceQuota;
import phoebe.eqx.pcef.core.data.ResourceResponse;
import phoebe.eqx.pcef.core.model.Quota;
import phoebe.eqx.pcef.core.model.Transaction;
import phoebe.eqx.pcef.enums.model.EQuota;
import phoebe.eqx.pcef.instance.AppInstance;

import phoebe.eqx.pcef.instance.CommitData;
import phoebe.eqx.pcef.instance.Config;
import phoebe.eqx.pcef.instance.PCEFInstance;
import phoebe.eqx.pcef.message.parser.res.OCFUsageMonitoringResponse;
import phoebe.eqx.pcef.utils.MessageFlow;
import phoebe.eqx.pcef.utils.PCEFUtils;

import java.util.*;


public class QuotaService extends MongoDBService {


    private Date minExpireDate;
    private boolean haveNewQuota;

    public QuotaService(AppInstance appInstance, MongoClient mongoClient) {
        super(appInstance, mongoClient, Config.COLLECTION_QUOTA_NAME);
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
                quota.setCounterId(resourceResponse.getCounterId());

                if (resourceResponse.getQuotaByKey() != null) {
                    quota.setQuotaByKey(resourceResponse.getQuotaByKey());
                    quota.setRateLimitByKey(resourceResponse.getRateLimitByKey());

                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.SECOND, resourceResponse.getQuotaByKey().getValidityTime());
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


    public void insertQuotaInitial(ArrayList<Quota> quotaResponses) {
        try {
            List<BasicDBObject> quotaBasicObjectList = getQuotaToBasicObjectList(quotaResponses);
            insertManyByObject(quotaBasicObjectList);
            this.minExpireDate = calMinExpireDate(quotaBasicObjectList);
            this.haveNewQuota = true;

            PCEFUtils.writeMessageFlow("Insert Quota Initial", MessageFlow.Status.Success, appInstance.getPcefInstance().getSessionId());
        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Insert Quota Initial error" + e.getStackTrace()[0], MessageFlow.Status.Error, appInstance.getPcefInstance().getSessionId());
            throw e;
        }

    }


    public void removeQuota(String privateId) {
        BasicDBObject delete = new BasicDBObject(EQuota.userValue.name(), privateId);
        db.getCollection(collectionName).remove(delete);
    }


    private void deleteQuotaByKey(String key) {
        BasicDBObject delete = new BasicDBObject(EQuota._id.name(), key);
        writeQueryLog("remove", collectionName, delete.toString());
        db.getCollection(collectionName).remove(delete);

    }


    public Map<String, String> getResourceIdMapMk(List<Quota> quotas) {
        Map<String, String> resourceIdMapMk = new HashMap<>();
        for (Quota quota : quotas) {
            for (ResourceQuota resourceQuota : quota.getResources()) {
                resourceIdMapMk.put(resourceQuota.getResourceId(), quota.getMonitoringKey());
            }
        }

        return resourceIdMapMk;
    }

    public List<String> getMkFromCommitData(List<CommitData> commitDataList) {
        List<String> mkCommits = new ArrayList<>();
        if (commitDataList.size() > 0) {
            for (CommitData commitData : commitDataList) {
                String mk = commitData.get_id().getMonitoringKey();
                if (!mkCommits.contains(mk)) {
                    mkCommits.add(mk);
                }
            }
        }
        return mkCommits;

    }

    public void updateQuota(ArrayList<Quota> quotaResponses) {

        try {


            List<BasicDBObject> quotaBasicObjectList = getQuotaToBasicObjectList(quotaResponses);
            List<BasicDBObject> newQuotaList = new ArrayList<>();

            List<CommitData> commitDataList = appInstance.getPcefInstance().getCommitDatas();


            //get monitoring key request
            List<String> mkCommits = getMkFromCommitData(commitDataList);

            //get monitoring key response
            List<String> mkResponses = new ArrayList<>();
            quotaResponses.forEach(quota -> mkResponses.add(quota.getMonitoringKey()));

            //##delete old quota
            List<String> mkUpdateCounter = new ArrayList<>();
            for (String mkCommit : mkCommits) {
                if (!mkResponses.contains(mkCommit)) {
                    //delete
                    deleteQuotaByKey(mkCommit);
                } else {
                    mkUpdateCounter.add(mkCommit);
                }
            }


            //##insert and update quota
            for (BasicDBObject quotaBasicObject : quotaBasicObjectList) {
                String mk = quotaBasicObject.get(EQuota.monitoringKey.name()).toString();

                if (quotaBasicObject.get(EQuota.quotaByKey.name()) != null) {  //receive new quota
                    if (commitDataList.size() == 0) {
                        //new quota --> insert
                        insertByQuery(quotaBasicObject);
                    } else {
                        if (!mkUpdateCounter.contains(mk)) {
                            //new quota --> insert
                            insertByQuery(quotaBasicObject);
                        } else {
                            //new counter(old mk) -->update set
                            BasicDBObject search = new BasicDBObject();
                            search.put(EQuota._id.name(), mk);
                            updateSetByQuery(search, quotaBasicObject);
                        }
                    }
                    newQuotaList.add(quotaBasicObject);
                } else {
                    // exist quota --> update push
                    BasicDBObject search = new BasicDBObject();
                    search.put(EQuota._id.name(), mk);
                    db.getCollection(Config.COLLECTION_QUOTA_NAME).update(search, new BasicDBObject("$push"
                            , new BasicDBObject(EQuota.resources.name()
                            , new BasicDBObject("$each", quotaBasicObject.get(EQuota.resources.name())))));

                }
            }

            if (newQuotaList.size() > 0) {
                this.haveNewQuota = true;
                this.minExpireDate = findQuotaGetMinimumExpireDate();
            }


            PCEFUtils.writeMessageFlow("Update Quota", MessageFlow.Status.Success, appInstance.getPcefInstance().getSessionId());
        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Update Quota error" + e.getStackTrace()[0], MessageFlow.Status.Error, appInstance.getPcefInstance().getSessionId());
            throw e;
        }


    }
/*
public void updateQuota(ArrayList<Quota> quotaResponses) {

        PCEFInstance pcefInstance = appInstance.getPcefInstance();

        List<BasicDBObject> quotaBasicObjectList = getQuotaToBasicObjectList(quotaResponses);
        List<BasicDBObject> newQuotaList = new ArrayList<>();


        List<Quota> quotaCommits = new ArrayList<>();
        if (pcefInstance.doCommit()) {
            Quota quotaExhaust = appInstance.getPcefInstance().getCommitPart().getQuotaExhaust();
            List<Quota> quotaExpireList = appInstance.getPcefInstance().getCommitPart().getQuotaExpireList();

            if (quotaExhaust != null) {
                quotaCommits.add(quotaExhaust);
            } else if (quotaExpireList.size() > 0) {
                quotaCommits.addAll(quotaExpireList);
            }
        }

        List<String> mkResponses = new ArrayList<>();
        quotaResponses.forEach(quota -> mkResponses.add(quota.getMonitoringKey()));

        //##delete old quota
        List<String> mkUpdateCounter = new ArrayList<>();
        for (Quota quota : quotaCommits) {
            String mk = quota.getMonitoringKey();
            if (!mkResponses.contains(mk)) {
                deleteQuotaByKey(mk);
            } else {
                mkUpdateCounter.add(mk);
            }
        }


        //##insert and update quota
        for (BasicDBObject quotaBasicObject : quotaBasicObjectList) {
            String mk = quotaBasicObject.get(EQuota.monitoringKey.name()).toString();

            if (quotaBasicObject.get(EQuota.quotaByKey.name()) != null) {  //receive new quota
                if (!pcefInstance.doCommit()) {
                    //new quota --> insert
                    insertByQuery(quotaBasicObject);
                } else {
                    if (!mkUpdateCounter.contains(mk)) {
                        //new quota --> insert
                        insertByQuery(quotaBasicObject);
                    } else {
                        //new counter -->update set
                        BasicDBObject search = new BasicDBObject();
                        search.put(EQuota._id.name(), mk);
                        updateSetByQuery(search, quotaBasicObject);
                    }
                }
                newQuotaList.add(quotaBasicObject);
            } else {
                // exist quota --> update push
                BasicDBObject search = new BasicDBObject();
                search.put(EQuota._id.name(), mk);
                db.getCollection(Config.COLLECTION_QUOTA_NAME).update(search, new BasicDBObject("$push"
                        , new BasicDBObject(EQuota.resources.name()
                        , new BasicDBObject("$each", quotaBasicObject.get(EQuota.resources.name())))));

            }
        }

        if (newQuotaList.size() > 0) {
            this.haveNewQuota = true;
            this.minExpireDate = findQuotaGetMinimumExpireDate();
        }

    }
*/


    public void filterTransactionConfirmIsNewResource(List<Transaction> otherTransaction) {
        int index = 0;
        for (Transaction transaction : otherTransaction) {
            DBCursor quotaCursor = findQuotaByTransaction(transaction);
            if (quotaCursor.hasNext()) {
                otherTransaction.remove(index);
            }
            index++;
        }
    }

    public DBCursor findQuotaByTransaction(Transaction transaction) {
        try {
            BasicDBObject searchQuery = new BasicDBObject();
            searchQuery.put(EQuota.userValue.name(), appInstance.getPcefInstance().getProfile().getUserValue());
            searchQuery.put(EQuota.resources.name(), new BasicDBObject("$elemMatch", new BasicDBObject("resourceId", transaction.getResourceId())));
            DBCursor dbCursor = findByQuery(searchQuery);

            if (dbCursor.hasNext()) {
                PCEFUtils.writeMessageFlow("Find Quota resource Id:" + transaction.getResourceId() + "[Found]", MessageFlow.Status.Success, appInstance.getPcefInstance().getSessionId());
            } else {
                PCEFUtils.writeMessageFlow("Find Quota resource Id:" + transaction.getResourceId() + "[Not Found]", MessageFlow.Status.Success, appInstance.getPcefInstance().getSessionId());
            }
            return dbCursor;
        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Find Quota resource Id:" + transaction.getResourceId() + "-error" + e.getStackTrace()[0], MessageFlow.Status.Error, appInstance.getPcefInstance().getSessionId());
            throw e;
        }
    }


    private List<Quota> getQuotaListFromDBCursor(DBCursor quotaCursor) {
        List<Quota> quotaList = new ArrayList<>();
        while (quotaCursor.hasNext()) {
            DBObject dbObject = quotaCursor.next();
            Quota quota = gson.fromJson(gson.toJson(dbObject), Quota.class);
            AFLog.d("mk:" + quota.getMonitoringKey() + ",expireDate :" + quota.getExpireDate());
            quotaList.add(quota);
        }
        return quotaList;
    }


    public List<Quota> findAllQuotaByPrivateId(String privateId) {
        BasicDBObject search = new BasicDBObject();
        search.put(EQuota.userValue.name(), privateId);
        DBCursor dbCursor = findByQuery(search);
        return getQuotaListFromDBCursor(dbCursor);
    }

    public List<Quota> findQuotaExpire() {
        Date currentTime = appInstance.getPcefInstance().getStartTime();

        BasicDBObject search = new BasicDBObject();
        search.put(EQuota.userValue.name(), appInstance.getPcefInstance().getProfile().getUserValue());
        search.put(EQuota.expireDate.name(), new BasicDBObject("$lte", currentTime));

        DBCursor dbCursor = findByQuery(search);

        AFLog.d("currentTime = " + currentTime);
        return getQuotaListFromDBCursor(dbCursor);


    }


    private Date calMinExpireDate(List<BasicDBObject> quotaBasicObjectList) {
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


 /*   public boolean checkQuotaAvailable(Quota quota, Map<String, Integer> countUnitByResourceMap, CommitPart commitPart) {
        int sumTransaction = countUnitByResourceMap.values().stream().mapToInt(count -> count).sum();

        int quotaUnit = quota.getQuotaByKey().getUnit();
        if (quotaUnit > sumTransaction) {
            AFLog.d("Quota Available");
            return true;
        } else {
            AFLog.d("Quota Exhaust");

            commitPart.setQuotaExhaust(quota);
            appInstance.getPcefInstance().setCommitPart(commitPart);
            return false;
        }
    }*/


    public Date findQuotaGetMinimumExpireDate() {
        String privateId = appInstance.getPcefInstance().getProfile().getUserValue();

        BasicDBObject match = new BasicDBObject();
        match.put(EQuota.userValue.name(), privateId);

        BasicDBObject group = new BasicDBObject();
        group.put("_id", new BasicDBObject());
        group.put("minExpireDate", new BasicDBObject("$min", "$" + EQuota.expireDate.name()));

        //find minimum
        Date minExpireDate = (Date) aggregateMatch(match, group).iterator().next().get("minExpireDate");
        AFLog.d("min ExpireDate:" + minExpireDate);

        return minExpireDate;
    }


    public DBObject findAndModifyLockQuota(String monitoringKey) {
        try {

            BasicDBObject query = new BasicDBObject();
            query.put(EQuota._id.name(), monitoringKey);
            query.put(EQuota.processing.name(), 0);

            BasicDBObject update = new BasicDBObject();
            update.put(EQuota.processing.name(), 1);//lock

            DBObject dbObject = findAndModify(query, update);
            PCEFUtils.writeMessageFlow("Find and Modify Quota Lock Process", MessageFlow.Status.Success, appInstance.getPcefInstance().getSessionId());

            return dbObject;
        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Find and Modify Quota Lock Process error " + e.getStackTrace()[0], MessageFlow.Status.Error, appInstance.getPcefInstance().getSessionId());
            throw e;
        }
    }

    public void updateUnLockQuota(String monitoringKey) {

        try {
            BasicDBObject query = new BasicDBObject();
            query.put(EQuota._id.name(), monitoringKey);
            query.put(EQuota.processing.name(), 1);

            BasicDBObject update = new BasicDBObject();
            update.put(EQuota.processing.name(), 0);//lock

            updateSetByQuery(query, update);

            PCEFUtils.writeMessageFlow("Update Quota Unlock Process mk:" + monitoringKey, MessageFlow.Status.Success, appInstance.getPcefInstance().getSessionId());
        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Update Quota Unlock Process mk:" + monitoringKey + ",error - " + e.getStackTrace()[0], MessageFlow.Status.Success, appInstance.getPcefInstance().getSessionId());
        }

    }


    public boolean findAndModifyLockQuotaExpire(List<Quota> quotaCommits) {
        boolean canProcess = true;
        for (Quota quota : quotaCommits) {
            if (quota.getProcessing() == 1) {
                continue;
            }

            //processing 0 --> 1
            DBObject dbObject = findAndModifyLockQuota(quota.getMonitoringKey());
            if (dbObject != null) {
                //success
                quota.setProcessing(1);
            } else {
                //not success do waiting
                canProcess = false;
                break;
            }
        }
        return canProcess;
    }


    /*  public boolean findAndModifyLockQuotaExpire() {
        boolean canProcess = true;
        for (Quota quota : quotaExpireList) {
            if (quota.getProcessing() == 1) {
                continue;
            }

            //processing 0 --> 1
            DBObject dbObject = findAndModifyLockQuota(quota.getMonitoringKey());
            if (dbObject != null) {
                //success
                quota.setProcessing(1);
            } else {
                //not success do waiting
                canProcess = false;
            }
        }
        return canProcess;
    }*/

    public Date getMinExpireDate() {
        return minExpireDate;
    }

    public boolean isHaveNewQuota() {
        return haveNewQuota;
    }
}
