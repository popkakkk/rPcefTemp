package phoebe.eqx.pcef.states.mongodb;


import com.mongodb.DBCursor;
import com.mongodb.DuplicateKeyException;
import phoebe.eqx.pcef.core.exceptions.TimeoutIntervalException;
import phoebe.eqx.pcef.enums.EMongoState;
import phoebe.eqx.pcef.enums.EState;
import phoebe.eqx.pcef.instance.Config;
import phoebe.eqx.pcef.model.Transaction;
import phoebe.eqx.pcef.services.MongoDBService;
import phoebe.eqx.pcef.states.mongodb.abs.MessageMongoRecieved;
import phoebe.eqx.pcef.states.mongodb.abs.MongoState;
import phoebe.eqx.pcef.utils.Interval;

public class W_MONGODB_PROCESS_STATE extends MongoState {


    public W_MONGODB_PROCESS_STATE(MongoDBService mongoDBService) {
        this.mongoDBService = mongoDBService;
    }

    private MongoDBService mongoDBService;
    private EState pcefNextState;
    private boolean responseSuccess;

    private Interval interval = new Interval(Config.RETRY_PROCESSING, Config.INTERVAL_PROCESSING);
    private Interval intervalInsertProfile = new Interval(Config.RETRY_PROCESSING, Config.INTERVAL_PROCESSING);
    private Interval intervalMkIsProcessing = new Interval(Config.RETRY_PROCESSING, Config.INTERVAL_PROCESSING);

    @MessageMongoRecieved(messageType = EMongoState.BEGIN)
    private void InitialProcess() {
        EMongoState nextState = null;
        try {
            DBCursor profileCursor = mongoDBService.findProfileByPrivateId();
            if (!profileCursor.hasNext()) {
                nextState = EMongoState.INSERT_PROFILE;
            } else {
                if (!mongoDBService.checkProfileIsProcessing(profileCursor)) {
                    if (mongoDBService.firstTimeAndWaitProcessing()) {
                        // find Monitoring key
//                            findMonitoringKeyState();
                    } else {
                        DBCursor transactionCursor = mongoDBService.findTransactionByStatus(Transaction.EStatus.Done.getName());
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
        }
        setNextState(nextState);
    }


    @MessageMongoRecieved(messageType = EMongoState.INSERT_PROFILE)
    private void insertProfileState() {
        EMongoState nextState = null;

        try {
            mongoDBService.insertLockProcess();
            setPcefNextState(EState.W_USAGE_MONITORING_START);
        } catch (DuplicateKeyException e) {
            mongoDBService.updateTransactionFirstTime();
            nextState = EMongoState.BEGIN;
        } catch (Exception e) {
            try {
                intervalInsertProfile.waitInterval();
                nextState = EMongoState.INSERT_PROFILE;
            } catch (TimeoutIntervalException ex) {
                setResponseFail();
            }

        }


        setNextState(nextState);
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


    public EState getPcefNextState() {
        return pcefNextState;
    }

    public void setPcefNextState(EState pcefNextState) {
        this.pcefNextState = pcefNextState;
    }

    public boolean isResponseSuccess() {
        return responseSuccess;
    }

    public void setResponseSuccess() {
        setPcefNextState(EState.END);
        this.responseSuccess = true;
    }


    public void setResponseFail() {
        setPcefNextState(EState.END);
        this.responseSuccess = false;
    }
}
