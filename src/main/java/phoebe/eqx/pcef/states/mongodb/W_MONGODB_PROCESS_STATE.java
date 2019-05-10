package phoebe.eqx.pcef.states.mongodb;


import com.mongodb.DBCursor;
import com.mongodb.DuplicateKeyException;
import phoebe.eqx.pcef.core.exceptions.TimeoutIntervalException;
import phoebe.eqx.pcef.enums.EStatusLifeCycle;
import phoebe.eqx.pcef.enums.state.EMongoState;
import phoebe.eqx.pcef.enums.state.EState;
import phoebe.eqx.pcef.instance.Config;
import phoebe.eqx.pcef.services.MongoDBService;
import phoebe.eqx.pcef.states.mongodb.abs.MessageMongoRecieved;
import phoebe.eqx.pcef.states.mongodb.abs.MongoState;
import phoebe.eqx.pcef.utils.Interval;

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
                if (mongoDBService.checkProfileIsProcessing(profileCursor)) {
                    if (mongoDBService.firstTimeAndWaitProcessing()) {
                        // find Monitoring key
//                            findMonitoringKeyState();
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


    @MessageMongoRecieved(messageType = EMongoState.PROCESS_RESOURCE)
    private void processResource() {
        try {
            DBCursor monitoringKeyCursor = mongoDBService.findMonitoringKey();
            if (!monitoringKeyCursor.hasNext()) {

            } else {
                if (!mongoDBService.checkMkIsProcessing(monitoringKeyCursor)) {
                    intervalMkIsProcessing.waitInterval();

                } else {


                }
            }
        } catch (Exception e) {

        }
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
