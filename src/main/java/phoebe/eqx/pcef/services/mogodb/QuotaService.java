package phoebe.eqx.pcef.services.mogodb;

import com.mongodb.*;
import ec02.af.utils.AFLog;
import phoebe.eqx.pcef.core.data.ResourceQuota;
import phoebe.eqx.pcef.core.data.ResourceResponse;
import phoebe.eqx.pcef.core.logs.summary.SummaryLog;
import phoebe.eqx.pcef.core.model.Quota;
import phoebe.eqx.pcef.core.model.Transaction;
import phoebe.eqx.pcef.enums.DBOperation;
import phoebe.eqx.pcef.enums.EError;
import phoebe.eqx.pcef.enums.Operation;
import phoebe.eqx.pcef.enums.model.EQuota;
import phoebe.eqx.pcef.enums.model.element.EResourceQuota;
import phoebe.eqx.pcef.enums.stats.EStatCmd;
import phoebe.eqx.pcef.enums.stats.EStatMode;
import phoebe.eqx.pcef.instance.AppInstance;

import phoebe.eqx.pcef.instance.CommitData;
import phoebe.eqx.pcef.instance.Config;
import phoebe.eqx.pcef.message.parser.res.OCFUsageMonitoringResponse;
import phoebe.eqx.pcef.utils.DBResult;
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
        List<BasicDBObject> quotaBasicObjectList = null;
        SummaryLog summaryLog;
        try {
            quotaBasicObjectList = getQuotaToBasicObjectList(quotaResponses);

            summaryLog = new SummaryLog(Operation.UpdateQuota.name(), new Date(), quotaBasicObjectList);
            context.getSummaryLogs().add(summaryLog);

            PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.sent_update_Quota_request);
            PCEFUtils.writeDBMessageRequest(collectionName, DBOperation.UPDATE, quotaBasicObjectList);

        } catch (Exception e) {
            context.setPcefException(PCEFUtils.getPCEFException(e, EError.UPDATE_QUOTA_REQUEST_ERROR));
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.ERROR, EStatCmd.sent_update_Quota_request);
            throw e;
        }

        try {
            WriteResult writeResult = insertManyByObject(quotaBasicObjectList);
            this.minExpireDate = calMinExpireDate(quotaBasicObjectList);
            this.haveNewQuota = true;

            BasicDBObject dbResLog = new BasicDBObject();
//            dbResLog.put("_id", quotaBasicObjectList.get("_id"));

            List<Object> objectList = (List) quotaBasicObjectList;
            PCEFUtils.writeDBMessageResponse(DBResult.SUCCESS, writeResult.getN(), objectList);

            summaryLog.getSummaryLogDetail().setResponse(new Date(), dbResLog);
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.receive_update_Quota_response);


        } catch (MongoTimeoutException e) {
            PCEFUtils.writeDBMessageResponseError();
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.TIMEOUT, EStatCmd.receive_update_Quota_response);
            context.setPcefException(PCEFUtils.getPCEFException(e, EError.UPDATE_PROFILE_RESPONSE_ERROR));
            throw e;
        } catch (MongoException e) {
            PCEFUtils.writeDBMessageResponseError();
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.ERROR, EStatCmd.receive_update_Quota_response);
            context.setPcefException(PCEFUtils.getPCEFException(e, EError.UPDATE_PROFILE_RESPONSE_ERROR));
            throw e;
        }
    }


    public void removeQuota(String privateId) {
        AFLog.d("Delete Quota with privateId:" + privateId + " ..");

        BasicDBObject delete = new BasicDBObject(EQuota.userValue.name(), privateId);
        writeQueryLog("remove", collectionName, delete);
        db.getCollection(collectionName).remove(delete);
    }


    private void deleteQuotaByKey(String monitoringKey, String userValue) {
        BasicDBObject delete = new BasicDBObject();
        delete.put(EQuota.monitoringKey.name(), monitoringKey);
        delete.put(EQuota.userValue.name(), userValue);

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


    public List<Quota> getQuotaForModify(List<CommitData> commitDataList) {

        List<Quota> quotaForModify = new ArrayList<>();
        List<String> mkCommits = getMkFromCommitData(commitDataList);
        mkCommits.forEach(s -> {
            Quota quota = new Quota();
            quota.setMonitoringKey(s);
            quota.setProcessing(0);
            quotaForModify.add(quota);
        });
        return quotaForModify;
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


    public void processQuotaResponse(ArrayList<Quota> quotaResponses) {
        //filter to check quota available


    }


    public void updateQuota(ArrayList<Quota> quotaResponses) {

        try {
            List<BasicDBObject> quotaBasicObjectList = getQuotaToBasicObjectList(quotaResponses);
            List<BasicDBObject> newQuotaList = new ArrayList<>();

            List<CommitData> commitDataList = context.getPcefInstance().getCommitDatas();

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
                    deleteQuotaByKey(mkCommit, context.getPcefInstance().getProfile().getUserValue());
                    AFLog.d("[QUOTA UPDATE] Delete Quota by monitoringKey:" + mkCommit);
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
                        AFLog.d("[QUOTA UPDATE] Insert New Quota monitoringKey:" + quotaBasicObject.get(EQuota.monitoringKey.name()));
                    } else {
                        if (!mkUpdateCounter.contains(mk)) {
                            //new quota --> insert
                            insertByQuery(quotaBasicObject);
                            AFLog.d("[QUOTA UPDATE] Insert New Quota monitoringKey:" + quotaBasicObject.get(EQuota.monitoringKey.name()));
                        } else {
                            //new counter(old mk) -->update set
                            BasicDBObject search = new BasicDBObject();
                            search.put(EQuota.monitoringKey.name(), mk);
                            search.put(EQuota.userValue.name(), context.getPcefInstance().getProfile().getUserValue());
                            updateSetByQuery(search, quotaBasicObject);
                            AFLog.d("[QUOTA UPDATE] Update Quota:" + quotaBasicObject);
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
                    AFLog.d("[QUOTA UPDATE] Quota Exits, Update Resource Quota:" + quotaBasicObject.get(EQuota.resources.name()));

                }
            }

            if (newQuotaList.size() > 0) {
                this.haveNewQuota = true;
                this.minExpireDate = findQuotaGetMinimumExpireDate();
                AFLog.d("[QUOTA UPDATE] have New Quota:" + haveNewQuota);
                AFLog.d("[QUOTA UPDATE] minExpireDate:" + PCEFUtils.isoDateFormatter.format(minExpireDate));
            }

        } catch (Exception e) {
            throw e;
        }
    }


    public void filterTransactionConfirmIsNewResource(List<Transaction> otherTransaction) {
        List<Transaction> filterTransaction = new ArrayList<>();
        for (Transaction transaction : otherTransaction) {
            AFLog.d("Confirm Resource Is New Resource..");
            DBCursor quotaCursor = findQuotaByTransaction(transaction);
            if (quotaCursor.hasNext()) {
                AFLog.d("[Confirm Resource Is New Resource] have quota:" + quotaCursor.next().get(EQuota.monitoringKey.name()) + ",filter tid:" + transaction.getTid());
                filterTransaction.add(transaction);
            }
        }
        otherTransaction.removeAll(filterTransaction);
    }

    public DBCursor findQuotaByTransaction(Transaction transaction) {
        try {
            BasicDBObject searchQuery = new BasicDBObject();
            searchQuery.put(EQuota.userValue.name(), context.getPcefInstance().getProfile().getUserValue());
//            searchQuery.put(EQuota.resources.name(), new BasicDBObject("$elemMatch", new BasicDBObject(EResourceQuota.resourceId.name(), transaction.getResourceId())));
            searchQuery.put(EQuota.resources.name() + "." + EResourceQuota.resourceId.name(), transaction.getResourceId());
            DBCursor dbCursor = findByQuery(searchQuery);

            if (dbCursor.hasNext()) {
            } else {
            }
            return dbCursor;
        } catch (Exception e) {
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
        Date currentTime = context.getPcefInstance().getStartTime();

        BasicDBObject search = new BasicDBObject();
        search.put(EQuota.userValue.name(), context.getPcefInstance().getProfile().getUserValue());
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
            context.getPcefInstance().setCommitPart(commitPart);
            return false;
        }
    }*/


    public Date findQuotaGetMinimumExpireDate() {
        String privateId = context.getPcefInstance().getProfile().getUserValue();

        BasicDBObject match = new BasicDBObject();
        match.put(EQuota.userValue.name(), privateId);

        BasicDBObject group = new BasicDBObject();
        group.put("_id", new BasicDBObject());
        group.put("minExpireDate", new BasicDBObject("$min", "$" + EQuota.expireDate.name()));

        //find minimum
        Date minExpireDate = (Date) aggregateMatch(match, group).iterator().next().get("minExpireDate");
//        AFLog.d("min ExpireDate:" + PCEFUtils.isoDateFormatter.format(minExpireDate));

        return minExpireDate;
    }


    public DBObject findAndModifyLockQuota(String monitoringKey, String userValue) {
        try {

            BasicDBObject query = new BasicDBObject();
            query.put(EQuota.monitoringKey.name(), monitoringKey);
            query.put(EQuota.userValue.name(), userValue);
            query.put(EQuota.processing.name(), 0);

            BasicDBObject update = new BasicDBObject();
            update.put(EQuota.processing.name(), 1);//lock

            DBObject dbObject = findAndModify(query, update);

            return dbObject;
        } catch (Exception e) {
            throw e;
        }
    }

    public void updateUnLockQuota(String monitoringKey, String userValue) {

        try {
            BasicDBObject query = new BasicDBObject();
            query.put(EQuota.monitoringKey.name(), monitoringKey);
            query.put(EQuota.userValue.name(), userValue);
            query.put(EQuota.processing.name(), 1);

            BasicDBObject update = new BasicDBObject();
            update.put(EQuota.processing.name(), 0);//lock

            updateSetByQuery(query, update);

        } catch (Exception e) {
        }

    }


    public boolean findAndModifyLockQuotaList(List<Quota> quotaCommits) {
        boolean canProcess = true;
        for (Quota quota : quotaCommits) {
            if (quota.getProcessing() == 1) {
                continue;
            }

            //processing 0 --> 1
            DBObject dbObject = findAndModifyLockQuota(quota.getMonitoringKey(),context.getPcefInstance().getProfile().getUserValue());
            if (dbObject != null) {
                //success
                quota.setProcessing(1);
            } else {
                //not success do waiting
                canProcess = false;
            }
        }

        if (canProcess) {
            //reset
            context.getPcefInstance().setQuotaModifyList(new ArrayList<>());
        }

        return canProcess;
    }


    /*  public boolean findAndModifyLockQuotaList() {
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
