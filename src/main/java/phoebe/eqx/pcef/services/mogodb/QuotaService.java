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

import phoebe.eqx.pcef.instance.Config;
import phoebe.eqx.pcef.instance.PCEFInstance;
import phoebe.eqx.pcef.message.parser.res.OCFUsageMonitoringResponse;

import java.util.*;


public class QuotaService extends MongoDBService {


    private Date minExpireDate;
    private boolean haveNewQuota;

    private List<Quota> quotaExpireList = new ArrayList<>();


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
                if (resourceResponse.getQuotaByKey() != null) {
                    quota.setCounterId(resourceResponse.getCounterId());
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


    public void insertQuotaFirstUsage(ArrayList<Quota> quotaResponses) {
        List<BasicDBObject> quotaBasicObjectList = getQuotaToBasicObjectList(quotaResponses);
        insertManyByObject(quotaBasicObjectList);
        this.minExpireDate = calMinExpireDate(quotaBasicObjectList);
    }


    public void removeQuota(String privateId) {
        BasicDBObject delete = new BasicDBObject(EQuota.userValue.name(), privateId);
        db.getCollection(collectionName).remove(delete);
    }


    private void deleteOldQuota(Quota oldQuota, List<Quota> quotaResponses) {

        String oldMk = oldQuota.getMonitoringKey();
        String newMk = null;

        //find new mk response by resourceId request
        for (Quota quota : quotaResponses) {
            if (newMk != null) {
                break;
            }
            for (ResourceQuota rsQuotaResponse : quota.getResources()) {
                if (oldQuota.getResources().get(0).getResourceId().equals(rsQuotaResponse.getResourceId())) { //[0] = check by some resource id from old quota
                    newMk = quota.getMonitoringKey();
                    break;
                }
            }
        }


        //old quota --> delete
        String action = "do not delete";
        if (!oldMk.equals(newMk)) {
            deleteQuotaByKey(oldMk);
            action = "delete";
        }
        AFLog.d("Old MK: " + oldMk + ",New MK: " + newMk + ",Action: " + action);
    }

    private void deleteQuotaByKey(String key) {
        db.getCollection(collectionName).remove(new BasicDBObject(EQuota._id.name(), key));

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

    public void updateQuota(ArrayList<Quota> quotaResponses) {

        PCEFInstance pcefInstance = appInstance.getPcefInstance();

        List<BasicDBObject> quotaBasicObjectList = getQuotaToBasicObjectList(quotaResponses);
        List<BasicDBObject> newQuotaList = new ArrayList<>();

     /*
        if (pcefInstance.doCommit()) {
            Quota quotaExhaust = appInstance.getPcefInstance().getCommitPart().getQuotaExhaust();
            List<Quota> quotaExpireList = appInstance.getPcefInstance().getCommitPart().getQuotaExpireList();

            if (quotaExhaust != null) {
                deleteOldQuota(quotaExhaust, quotaResponses);
            } else if (quotaExpireList.size() > 0) {
                quotaExpireList.forEach(quota -> deleteOldQuota(quota, quotaResponses));
            }
        }*/


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
                db.getCollection(collectionName).update(search, new BasicDBObject("$push"
                        , new BasicDBObject(EQuota.resources.name()
                        , new BasicDBObject("$each", EQuota.resources.name()))));

            }
        }

        if (newQuotaList.size() > 0) {
            this.haveNewQuota = true;
            this.minExpireDate = findQuotaGetMinimumExpireDate();
        }

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

    public DBCursor findQuotaByTransaction(Transaction transaction) {
        BasicDBObject searchQuery = new BasicDBObject();
        searchQuery.put(EQuota.userValue.name(), appInstance.getPcefInstance().getProfile().getUserValue());
        searchQuery.put(EQuota.resources.name(), new BasicDBObject("$elemMatch", new BasicDBObject("resourceId", transaction.getResourceId())));
        return findByQuery(searchQuery);
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


    public List<Quota> findAllQuotaByPrivateId() {
        BasicDBObject search = new BasicDBObject();
        search.put(EQuota.userValue.name(), appInstance.getPcefInstance().getProfile().getUserValue());
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
        return (Date) aggregateMatch(match, group).iterator().next().get("minExpireDate");
    }


    public DBObject findAndModifyLockQuota(String monitoringKey) {
        BasicDBObject query = new BasicDBObject();
        query.put(EQuota._id.name(), monitoringKey);
        query.put(EQuota.processing.name(), 0);

        BasicDBObject update = new BasicDBObject();
        update.put(EQuota.processing.name(), 1);//lock

        return findAndModify(query, update);
    }


    public boolean findAndModifyLockQuotaExpire() {
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
    }

    public Date getMinExpireDate() {
        return minExpireDate;
    }

    public boolean isHaveNewQuota() {
        return haveNewQuota;
    }

    public List<Quota> getQuotaExpireList() {
        return quotaExpireList;
    }

    public void setQuotaExpireList(List<Quota> quotaExpireList) {
        this.quotaExpireList = quotaExpireList;
    }
}
