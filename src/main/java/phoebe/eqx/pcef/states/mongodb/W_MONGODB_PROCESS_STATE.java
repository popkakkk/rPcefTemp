package phoebe.eqx.pcef.states.mongodb;


import com.google.gson.Gson;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.DuplicateKeyException;
import ec02.af.utils.AFLog;
import phoebe.eqx.pcef.core.exceptions.TimeoutIntervalException;
import phoebe.eqx.pcef.core.model.Quota;
import phoebe.eqx.pcef.enums.state.EMongoState;
import phoebe.eqx.pcef.enums.state.EState;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.instance.CommitPart;
import phoebe.eqx.pcef.instance.Config;
import phoebe.eqx.pcef.services.mogodb.MongoDBConnect;
import phoebe.eqx.pcef.states.mongodb.abs.MessageMongoRecieved;
import phoebe.eqx.pcef.states.mongodb.abs.MongoState;
import phoebe.eqx.pcef.utils.Interval;

import java.util.ArrayList;
import java.util.Map;

public class W_MONGODB_PROCESS_STATE extends MongoState {


    private boolean responseSuccess;

    private boolean waitForProcess;
    private boolean lockByMyTransaction;

    private Interval interval = new Interval(Config.RETRY_PROCESSING, Config.INTERVAL_PROCESSING);
    private Interval intervalInsertProfile = new Interval(Config.RETRY_PROCESSING, Config.INTERVAL_PROCESSING);
    private Interval intervalMkIsProcessing = new Interval(Config.RETRY_PROCESSING, Config.INTERVAL_PROCESSING);

    public W_MONGODB_PROCESS_STATE(AppInstance appInstance, MongoDBConnect dbConnect) {
        super(appInstance, dbConnect);
    }

    @MessageMongoRecieved(messageType = EMongoState.BEGIN)
    public EMongoState InitialProcess() {
        EMongoState nextState = null;
        try {
            DBCursor profileCursor = dbConnect.getProfileService().findProfileByPrivateId(appInstance.getPcefInstance().getUsageMonitoringRequest().getUserValue());


            if (!profileCursor.hasNext()) {
                nextState = EMongoState.INSERT_PROFILE;
            } else {
                nextState = EMongoState.FIND_QUOTA_BY_NEW_RESOURCE;
            }
        } catch (Exception e) {

        }
        return nextState;
    }


    @MessageMongoRecieved(messageType = EMongoState.INSERT_PROFILE)
    public EMongoState insertProfileState() {
        EMongoState nextState;
        try {
            dbConnect.getProfileService().insertProfile();
            setPcefState(EState.W_USAGE_MONITORING_START);
            nextState = EMongoState.END;
        } catch (DuplicateKeyException e) {
            nextState = EMongoState.FIND_QUOTA_BY_NEW_RESOURCE;
        } catch (Exception e) {
            try {
                intervalInsertProfile.waitInterval();
                nextState = EMongoState.INSERT_PROFILE;
            } catch (TimeoutIntervalException ex) {
                setResponseFail();
                nextState = EMongoState.END;
            }

        }
        return nextState;
    }


    @MessageMongoRecieved(messageType = EMongoState.FIND_QUOTA_BY_NEW_RESOURCE)
    public EMongoState findQuotaByNewResource() {
        EMongoState nextState = null;
        waitForProcess = false;
        try {
            DBCursor QuotaCursor = dbConnect.getQuotaService().findQuotaByTransaction(appInstance.getPcefInstance().getTransaction());
            if (!QuotaCursor.hasNext()) {
                nextState = EMongoState.FIND_AND_MOD_PROFILE_FOR_WAIT_PROCESS;
            } else {
                if (dbConnect.getQuotaService().checkMkCanProcess(QuotaCursor)) {
                    Quota quota = new Gson().fromJson(new Gson().toJson(QuotaCursor.iterator().next()), Quota.class);


                    /**
                     * **Check Quota Exhaust**
                     * **/

                    ArrayList<Quota> myQuotaToList = new ArrayList<>();
                    myQuotaToList.add(quota);


                    Map<String, Integer> countUnitByResourceMap = dbConnect.getTransactionService().findTransactionDoneGroupByResource(myQuotaToList);

                    int sumTransaction = countUnitByResourceMap.values().stream().mapToInt(count -> count).sum();
                    int quotaUnit = quota.getQuotaByKey().getUnit();

                    if (quotaUnit > sumTransaction) {
                        AFLog.d("Quota Available");
                        dbConnect.getTransactionService().updateTransaction(myQuotaToList);

                        setResponseSuccess();
                        nextState = EMongoState.END;
                    } else {
                        AFLog.d("Quota Exhaust");
                        CommitPart commitPart = new CommitPart();
                        commitPart.setCountUnitMap(countUnitByResourceMap);
                        commitPart.setQuotaExhaust(quota);
                        appInstance.getPcefInstance().setCommitPart(commitPart);

                        DBObject findModQuota = dbConnect.getQuotaService().findAndModifyLockQuota(quota.getMonitoringKey());
                        if (findModQuota != null) {
                            nextState = EMongoState.FIND_AND_MOD_PROFILE_FOR_UPDATE_QUOTA_EXHAUST;
                        } else {
                            //interval
                            nextState = EMongoState.FIND_QUOTA_BY_NEW_RESOURCE;
                        }
                    }


                } else {
                    intervalMkIsProcessing.waitInterval();
                    nextState = EMongoState.FIND_QUOTA_BY_NEW_RESOURCE;
                }
            }
        } catch (Exception e) {

        }
        return nextState;
    }

    @MessageMongoRecieved(messageType = EMongoState.FIND_AND_MOD_PROFILE_FOR_WAIT_PROCESS)
    public EMongoState findAndModProfileForWaitProcess() {
        EMongoState nextState = null;
        try {
            if (lockByMyTransaction) {
                setPcefState(EState.W_USAGE_MONITORING_UPDATE);
                nextState = EMongoState.END;
            } else {
                DBObject dbObject = dbConnect.getProfileService().findAndModifyLockProfile(appInstance.getPcefInstance().getProfile().getUserValue());
                if (dbObject != null) {
                    if (!waitForProcess) { // not wait process --> is new resource --> sent start by resource
                        setPcefState(EState.W_USAGE_MONITORING_UPDATE);
                        nextState = EMongoState.END;
                    } else {
                        if (dbConnect.getTransactionService().findMyTransactionDone()) {
                            dbConnect.getProfileService().updateProfileUnLock(dbConnect.getQuotaService().isHaveNewQuota(), dbConnect.getQuotaService().getMinExpireDate());
                            setResponseSuccess();
                        } else {
                            nextState = EMongoState.FIND_QUOTA_BY_NEW_RESOURCE;
                        }
                    }
                } else {
                    lockByMyTransaction = true;
                    waitForProcess = true;
                    interval.waitInterval();
                    nextState = EMongoState.FIND_AND_MOD_PROFILE_FOR_WAIT_PROCESS;
                }
            }

        } catch (Exception e) {

        }
        return nextState;
    }

    @MessageMongoRecieved(messageType = EMongoState.FIND_AND_MOD_PROFILE_FOR_UPDATE_QUOTA_EXHAUST)
    public EMongoState findProfileForUpdateQuotaExhaust() {
        EMongoState nextState = null;
        try {
            DBObject dbObject = dbConnect.getProfileService().findAndModifyLockProfile(appInstance.getPcefInstance().getTransaction().getUserValue());
            if (dbObject != null) {
                setPcefState(EState.W_USAGE_MONITORING_UPDATE);
                nextState = EMongoState.END;
            } else {
                //interval
                waitForProcess = true;
                interval.waitInterval();
                nextState = EMongoState.FIND_AND_MOD_PROFILE_FOR_UPDATE_QUOTA_EXHAUST;
            }

        } catch (Exception e) {

        }
        return nextState;
    }

    public boolean isResponseSuccess() {
        return responseSuccess;
    }

    public void setResponseSuccess() {
        setPcefState(EState.END);
        this.responseSuccess = true;
    }


    public void setResponseFail() {
        setPcefState(EState.END);
        this.responseSuccess = false;
    }

}
