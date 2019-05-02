package phoebe.eqx.pcef.states.L2;

import com.mongodb.DBCursor;
import com.mongodb.DuplicateKeyException;
import phoebe.eqx.pcef.core.exceptions.TimeoutIntervalException;
import phoebe.eqx.pcef.enums.EState;
import phoebe.eqx.pcef.instance.Config;
import phoebe.eqx.pcef.services.MongoDBService2;
import phoebe.eqx.pcef.utils.Interval;

public class MongoDBFirstState {

    private MongoDBService2 mongoDBService;
    private String status;
    private EState nextState;

    private Interval intervalIsProcessingFirst = new Interval(Config.RETRY_PROCESSING, Config.INTERVAL_PROCESSING);
    private Interval intervalInsertLockProcess = new Interval(Config.RETRY_PROCESSING, Config.INTERVAL_PROCESSING);
    private Interval intervalMkIsProcessing = new Interval(Config.RETRY_PROCESSING, Config.INTERVAL_PROCESSING);
    private Interval intervalIsProcessingForInitial = new Interval(Config.RETRY_PROCESSING, Config.INTERVAL_PROCESSING);
    private Interval intervalIsProcessingForUpdateQuota = new Interval(Config.RETRY_PROCESSING, Config.INTERVAL_PROCESSING);

    public MongoDBFirstState(MongoDBService2 mongoDBService) {
        this.mongoDBService = mongoDBService;
    }


    public void start() {
        mongoDBService.insertTransaction("1234");
        findProfileFirstState();
    }

    private void findProfileFirstState() {
        try {
            DBCursor profileCursor = mongoDBService.findLockProcess();
            if (!profileCursor.hasNext()) {
                insertLockProcessState();
            } else {
                if (!mongoDBService.checkIsProcessing(profileCursor)) {
                    intervalIsProcessingFirst.waitInterval();
                    findProfileFirstState();
                } else {

                    // if firsttime and wait process
                    if (true) {


                        // find col_transaction
                        DBCursor transactionCursor = mongoDBService.findTransaction();
                        if (transactionCursor.hasNext()) {
                            // responsee 2000 to SACF
                            //end
                        } else {
                            // find Monitoring key
                            findMonitoringKeyState();
                        }
                    }

                }
            }
        } catch (TimeoutIntervalException e) {
            //kill
        }

    }

    private void insertLockProcessState() {
        try {
            mongoDBService.insertLockProcess();
            setStatus("Success");
            setNextState(EState.W_USAGE_MONITORING_START);
        } catch (DuplicateKeyException e) {
            findProfileFirstState();
        } catch (Exception e) {
            try {
                intervalInsertLockProcess.waitInterval();
                insertLockProcessState();
            } catch (TimeoutIntervalException ex) {
                //kill
            }
        }
    }

    private void findMonitoringKeyState() {
        try {
            DBCursor monitoringKeyCursor = mongoDBService.findMonitoringKey();
            if (!monitoringKeyCursor.hasNext()) {
                findLockProcessForInitialMonitoringKeyState();
            } else {
                if (!mongoDBService.checkMkIsProcessing(monitoringKeyCursor)) {
                    intervalMkIsProcessing.waitInterval();
                    findMonitoringKeyState();
                } else {


                }
            }
        } catch (Exception e) {

        }
    }

    private void findLockProcessForInitialMonitoringKeyState() {
        try {

            DBCursor profileCursor = mongoDBService.findLockProcess();
            if (profileCursor.hasNext()) {
                if (!mongoDBService.checkIsProcessing(profileCursor)) {
                    intervalIsProcessingForInitial.waitInterval();
                    findLockProcessForInitialMonitoringKeyState();
                } else {
                    //set isProcessing = 1
                    //find Transaction
                    //
                    //sent usageMonitoring
                }
            }


        } catch (Exception e) {

        }
    }


    private void countTransactionState() {
        try {
            if (mongoDBService.countTransaction()) {
                //update transaction
                //response 200 to SACF
            } else {


            }

        } catch (Exception e) {

        }
    }


    private void findLockProcessForUpdateQuota() {
        try {

            DBCursor profileCursor = mongoDBService.findLockProcess();
            if (profileCursor.hasNext()) {
                if (!mongoDBService.checkIsProcessing(profileCursor)) {
                    intervalIsProcessingForUpdateQuota.waitInterval();
                    findLockProcessForUpdateQuota();
                } else {
                    //update lock process 1
                    //update transaction status processing
                    //find Transaction
                    //
                    //sent usageMonitoring Update
                }
            }


        } catch (Exception e) {

        }
    }


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public EState getNextState() {
        return nextState;
    }

    public void setNextState(EState nextState) {
        this.nextState = nextState;
    }
}
