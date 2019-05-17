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
import phoebe.eqx.pcef.services.mogodb.MongoDBService;
import phoebe.eqx.pcef.states.mongodb.abs.MessageMongoRecieved;
import phoebe.eqx.pcef.states.mongodb.abs.MongoState;
import phoebe.eqx.pcef.utils.Interval;

import java.util.ArrayList;

public class W_MONGODB_PROCESS_STATE extends MongoState {


    public W_MONGODB_PROCESS_STATE(MongoDBService mongoDBService) {
        this.mongoDBService = mongoDBService;
    }

    private MongoDBService mongoDBService;
    private EState usageMonitoringState;
    private boolean responseSuccess;

    private boolean waitForProcess;
    private boolean lockByMyTransaction;

    private Interval interval = new Interval(Config.RETRY_PROCESSING, Config.INTERVAL_PROCESSING);
    private Interval intervalInsertProfile = new Interval(Config.RETRY_PROCESSING, Config.INTERVAL_PROCESSING);
    private Interval intervalMkIsProcessing = new Interval(Config.RETRY_PROCESSING, Config.INTERVAL_PROCESSING);

    @MessageMongoRecieved(messageType = EMongoState.BEGIN)
    public EMongoState InitialProcess() {
        EMongoState nextState = null;
        try {
            DBCursor profileCursor = mongoDBService.findProfileByPrivateId();
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
            mongoDBService.insertProfile();
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
            DBCursor QuotaCursor = mongoDBService.findQuotaByThisTransaction();
            if (!QuotaCursor.hasNext()) {
                nextState = EMongoState.FIND_AND_MOD_PROFILE_FOR_WAIT_PROCESS;
            } else {
                if (mongoDBService.checkMkCanProcess(QuotaCursor)) {
                    Quota quota = new Gson().fromJson(new Gson().toJson(QuotaCursor.iterator().next()), Quota.class);

                    //count
                    boolean checkQuotaAvailable = mongoDBService.checkQuotaAvailable(quota);
                    if (checkQuotaAvailable) {
                        ArrayList<Quota> quotaList = new ArrayList<>();
                        quotaList.add(quota);
                        mongoDBService.updateTransaction(quotaList);

                        setResponseSuccess();
                    } else {
                        DBObject findModQuota = mongoDBService.findAndModifyLockQuota(quota.getMonitoringKey());
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
                DBObject dbObject = mongoDBService.findAndModifyLockProfile();
                if (dbObject != null) {
                    if (!waitForProcess) { // not wait process --> is new resource --> sent start by resource
                        setUsageMonitoringState(EState.W_USAGE_MONITORING_UPDATE);
                        nextState = EMongoState.END;
                    } else {
                        if (mongoDBService.findMyTransactionDone()) {
                            mongoDBService.updateProfileUnLock();
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
            DBObject dbObject = mongoDBService.findAndModifyLockProfile();
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


    public EState getUsageMonitoringState() {
        return usageMonitoringState;
    }

    public void setUsageMonitoringState(EState usageMonitoringState) {
        this.usageMonitoringState = usageMonitoringState;
    }

    public boolean isResponseSuccess() {
        return responseSuccess;
    }

    public void setResponseSuccess() {
        setUsageMonitoringState(EState.END);
        this.responseSuccess = true;
    }


    public void setResponseFail() {
        setUsageMonitoringState(EState.END);
        this.responseSuccess = false;
    }
}
