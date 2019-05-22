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
import phoebe.eqx.pcef.enums.config.EConfig;
import phoebe.eqx.pcef.enums.model.EQuota;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.instance.Config;
import phoebe.eqx.pcef.instance.PCEFInstance;
import phoebe.eqx.pcef.message.parser.res.OCFUsageMonitoringResponse;

import java.util.*;


public class QuotaService extends MongoDBService {


    private Date minExpireDate;
    private boolean haveNewQuota;
    List<Quota> quotaExpireList = new ArrayList<>();


    public QuotaService(AppInstance appInstance, MongoClient mongoClient) {
        super(appInstance, mongoClient,Config.COLLECTION_QUOTA_NAME);
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


    public void removeQuota(String privateId) {
        BasicDBObject delete = new BasicDBObject(EQuota.userValue.name(), privateId);
        db.getCollection(Config.COLLECTION_QUOTA_NAME).remove(delete);
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


    public DBCursor findQuotaByThisTransaction() {
        return findQuotaByTransaction(appInstance.getPcefInstance().getTransaction());
    }


    private DBCursor findQuotaByTransaction(Transaction transaction) {
        BasicDBObject searchQuery = new BasicDBObject();
        searchQuery.put(EQuota.userValue.name(), transaction.getUserValue());
        searchQuery.put(EQuota.resources.name(), new BasicDBObject("$elemMatch", new BasicDBObject("resourceId", transaction.getResourceId())));
        return findByQuery(Config.COLLECTION_QUOTA_NAME, searchQuery);
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
        DBCursor dbCursor = findByQuery(Config.COLLECTION_QUOTA_NAME, search);
        return getQuotaListFromDBCursor(dbCursor);
    }

    public List<Quota> findQuotaExpire() {
        Date currentTime = appInstance.getPcefInstance().getStartTime();

        BasicDBObject search = new BasicDBObject();
        search.put(EQuota.userValue.name(), appInstance.getPcefInstance().getProfile().getUserValue());
        search.put(EQuota.expireDate.name(), new BasicDBObject("$lte", currentTime));
        DBCursor dbCursor = findByQuery(Config.COLLECTION_QUOTA_NAME, search);
        AFLog.d("currentTime = " + currentTime);
        List<Quota> quotaExpireList = getQuotaListFromDBCursor(dbCursor);
        this.quotaExpireList = quotaExpireList;
        appInstance.getPcefInstance().setQuotaExpire(quotaExpireList);

        return quotaExpireList;
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


    public boolean checkQuotaAvailable(Quota quota, Map<String, Integer> countUnitByResourceMap) {
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


    public DBObject findAndModifyLockQuota(String monitoringKey) {
        BasicDBObject query = new BasicDBObject();
        query.put(EQuota._id.name(), monitoringKey);
        query.put(EQuota.processing.name(), 0);

        BasicDBObject update = new BasicDBObject();
        update.put(EQuota.processing.name(), 1);//lock

        return findAndModify(Config.COLLECTION_QUOTA_NAME, query, update);
    }


    public boolean findAndModifyLockQuotaExpire() {
        boolean canProcess = true;
        for (Quota quota : appInstance.getPcefInstance().getQuotaExpire()) {
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
}
