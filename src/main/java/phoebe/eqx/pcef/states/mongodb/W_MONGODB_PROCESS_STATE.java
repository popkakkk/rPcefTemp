package phoebe.eqx.pcef.states.mongodb;


import com.google.gson.Gson;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.DuplicateKeyException;
import phoebe.eqx.pcef.core.exceptions.TimeoutIntervalException;
import phoebe.eqx.pcef.enums.EStatusLifeCycle;
import phoebe.eqx.pcef.enums.state.EMongoState;
import phoebe.eqx.pcef.enums.state.EState;
import phoebe.eqx.pcef.instance.Config;
import phoebe.eqx.pcef.model.Quota;
import phoebe.eqx.pcef.services.mogodb.MongoDBService;
import phoebe.eqx.pcef.states.mongodb.abs.MessageMongoRecieved;
import phoebe.eqx.pcef.states.mongodb.abs.MongoState;
import phoebe.eqx.pcef.utils.Interval;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class W_MONGODB_PROCESS_STATE extends MongoState {


    public W_MONGODB_PROCESS_STATE(MongoDBService mongoDBService) {
        this.mongoDBService = mongoDBService;
    }

    private MongoDBService mongoDBService;
    private EState usageMonitoringState;
    private boolean responseSuccess;

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
                if (mongoDBService.checkCanProcessProfile(profileCursor)) {
                    if (mongoDBService.firstTimeAndWaitProcessing()) {
                        nextState = EMongoState.FIND_QUOTA_START_BY_RESOURCE;
                    } else {
                        DBCursor transactionCursor = mongoDBService.findTransactionByStatus(EStatusLifeCycle.Done.getName());
                        if (transactionCursor.hasNext()) {
                            setResponseSuccess();
                        } else {
                            // find Monitoring key
//                            findMonitoringKeyState();
                        }
                    }
                } else {
                    interval.waitInterval();
                    nextState = EMongoState.BEGIN;
                }
            }
        } catch (TimeoutIntervalException e) {
            setResponseFail();
            nextState = EMongoState.END;
        } catch (ParseException e) {


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
            mongoDBService.updateTransactionFirstTime();
            nextState = EMongoState.BEGIN;
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


    @MessageMongoRecieved(messageType = EMongoState.FIND_QUOTA_START_BY_RESOURCE)
    public EMongoState findQuotaStartByResource() {
        EMongoState nextState = null;
        try {
            DBCursor QuotaCursor = mongoDBService.findQuotaByResource();
            if (!QuotaCursor.hasNext()) {
                nextState = EMongoState.FIND_PROFILE_FOR_START_RESOURCE;
            } else {
                if (mongoDBService.checkMkCanProcess(QuotaCursor)) {
                    Quota quota = new Gson().fromJson(new Gson().toJson(QuotaCursor.iterator().next()), Quota.class);

                    boolean checkQuotaAvailable = mongoDBService.checkQuotaAvailable(quota);
                    if (checkQuotaAvailable) {
                        ArrayList<Quota> quotaList = new ArrayList<>();
                        quotaList.add(quota);
                        mongoDBService.updateTransactionSetQuota(quotaList);

                        //response success
                        setResponseSuccess();
                    } else {
                        //reserve

                    }


                } else {
                    intervalMkIsProcessing.waitInterval();
                    nextState = EMongoState.FIND_QUOTA_START_BY_RESOURCE;
                }
            }
        } catch (Exception e) {

        }
        return nextState;
    }

    @MessageMongoRecieved(messageType = EMongoState.FIND_PROFILE_FOR_START_RESOURCE)
    public EMongoState findProfileForStartResource() {
        EMongoState nextState = null;
        try {
            DBObject dbObject = mongoDBService.findAndModifyProfile();
            if (dbObject != null) {
                setUsageMonitoringState(EState.W_USAGE_MONITORING_UPDATE);
                nextState = EMongoState.END;
            } else {
                //interval
                nextState = EMongoState.FIND_PROFILE_FOR_START_RESOURCE;
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
