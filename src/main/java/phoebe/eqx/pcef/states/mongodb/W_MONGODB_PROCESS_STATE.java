package phoebe.eqx.pcef.states.mongodb;


import com.google.gson.Gson;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.DuplicateKeyException;
import phoebe.eqx.pcef.core.exceptions.TimeoutIntervalException;
import phoebe.eqx.pcef.core.model.Quota;
import phoebe.eqx.pcef.enums.state.EMongoState;
import phoebe.eqx.pcef.enums.state.EState;
import phoebe.eqx.pcef.instance.Config;
import phoebe.eqx.pcef.services.mogodb.MongoDBConnect;
import phoebe.eqx.pcef.states.mongodb.abs.MessageMongoRecieved;
import phoebe.eqx.pcef.states.mongodb.abs.MongoState;
import phoebe.eqx.pcef.utils.Interval;

import java.util.ArrayList;
import java.util.Map;

public class W_MONGODB_PROCESS_STATE extends MongoState {


    public W_MONGODB_PROCESS_STATE(MongoDBConnect dbConnect) {
        this.dbConnect = dbConnect;
    }

    private MongoDBConnect dbConnect;


    private boolean waitForProcess;
    private boolean lockByMyTransaction;

    private Interval interval = new Interval(Config.RETRY_PROCESSING, Config.INTERVAL_PROCESSING);
    private Interval intervalInsertProfile = new Interval(Config.RETRY_PROCESSING, Config.INTERVAL_PROCESSING);
    private Interval intervalMkIsProcessing = new Interval(Config.RETRY_PROCESSING, Config.INTERVAL_PROCESSING);

    @MessageMongoRecieved(messageType = EMongoState.BEGIN)
    public EMongoState InitialProcess() {
        EMongoState nextState = null;
        try {
            DBCursor profileCursor = dbConnect.getProfileService().findProfileByPrivateId();
            if (!profileCursor.hasNext()) {
                nextState = EMongoState.INSERT_PROFILE;
            } else {
                nextState = EMongoState.FIND_QUOTA_BY_RESOURCE;
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
            setUsageMonitoringState(EState.W_USAGE_MONITORING_START);
            nextState = EMongoState.END;
        } catch (DuplicateKeyException e) {
            nextState = EMongoState.FIND_QUOTA_BY_RESOURCE;
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


    @MessageMongoRecieved(messageType = EMongoState.FIND_QUOTA_BY_RESOURCE)
    public EMongoState findQuotatByResource() {
        EMongoState nextState = null;
        waitForProcess = false;
        try {
            DBCursor QuotaCursor = dbConnect.getQuotaService().findQuotaByThisTransaction();
            if (!QuotaCursor.hasNext()) {
                nextState = EMongoState.FIND_AND_MOD_PROFILE_FOR_WAIT_PROCESS;
            } else {
                if (dbConnect.getQuotaService().checkMkCanProcess(QuotaCursor)) {
                    Quota quota = new Gson().fromJson(new Gson().toJson(QuotaCursor.iterator().next()), Quota.class);

                    //count
                    Map<String, Integer> countUnitByResourceMap = dbConnect.getTransactionService().findTransactionDoneGroupByResource(quota);
                    boolean checkQuotaAvailable = dbConnect.getQuotaService().checkQuotaAvailable(quota, countUnitByResourceMap);
                    if (checkQuotaAvailable) {
                        ArrayList<Quota> quotaList = new ArrayList<>();
                        quotaList.add(quota);
                        dbConnect.getTransactionService().updateTransaction(quotaList);

                        setResponseSuccess();
                    } else {
                        DBObject findModQuota = dbConnect.getQuotaService().findAndModifyLockQuota(quota.getMonitoringKey());
                        if (findModQuota != null) {
                            nextState = EMongoState.FIND_AND_MOD_PROFILE_FOR_UPDATE_RESOURCE;
                        } else {
                            //interval
                            nextState = EMongoState.FIND_QUOTA_BY_RESOURCE;
                        }
                    }

                } else {
                    intervalMkIsProcessing.waitInterval();
                    nextState = EMongoState.FIND_QUOTA_BY_RESOURCE;
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
                setUsageMonitoringState(EState.W_USAGE_MONITORING_UPDATE);
                nextState = EMongoState.END;
            } else {
                DBObject dbObject = dbConnect.getProfileService().findAndModifyLockProfile();
                if (dbObject != null) {
                    if (!waitForProcess) { // not wait process --> is new resource --> sent start by resource
                        setUsageMonitoringState(EState.W_USAGE_MONITORING_UPDATE);
                        nextState = EMongoState.END;
                    } else {
                        if (dbConnect.getTransactionService().findMyTransactionDone()) {
                            dbConnect.getProfileService().updateProfileUnLock();
                            setResponseSuccess();
                        } else {
                            nextState = EMongoState.FIND_QUOTA_BY_RESOURCE;
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

    @MessageMongoRecieved(messageType = EMongoState.FIND_AND_MOD_PROFILE_FOR_UPDATE_RESOURCE)
    public EMongoState findProfileForUpdateResource() {
        EMongoState nextState = null;
        try {
            DBObject dbObject = dbConnect.getProfileService().findAndModifyLockProfile();
            if (dbObject != null) {
                setUsageMonitoringState(EState.W_USAGE_MONITORING_UPDATE);
                nextState = EMongoState.END;
            } else {
                //interval
                waitForProcess = true;
                interval.waitInterval();
                nextState = EMongoState.FIND_AND_MOD_PROFILE_FOR_UPDATE_RESOURCE;
            }

        } catch (Exception e) {

        }
        return nextState;
    }


}
