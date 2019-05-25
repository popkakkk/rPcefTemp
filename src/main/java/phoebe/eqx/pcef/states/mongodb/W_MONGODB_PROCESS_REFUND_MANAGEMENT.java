package phoebe.eqx.pcef.states.mongodb;


import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import ec02.af.utils.AFLog;
import phoebe.eqx.pcef.core.model.Transaction;
import phoebe.eqx.pcef.enums.EStatusLifeCycle;
import phoebe.eqx.pcef.enums.state.EMongoState;
import phoebe.eqx.pcef.enums.state.EState;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.instance.Config;
import phoebe.eqx.pcef.services.mogodb.MongoDBConnect;
import phoebe.eqx.pcef.states.mongodb.abs.MessageMongoRecieved;
import phoebe.eqx.pcef.states.mongodb.abs.MongoState;
import phoebe.eqx.pcef.utils.Interval;

public class W_MONGODB_PROCESS_REFUND_MANAGEMENT extends MongoState {


    private Interval interval = new Interval(Config.RETRY_PROCESSING, Config.INTERVAL_PROCESSING);

    private boolean success;

    public W_MONGODB_PROCESS_REFUND_MANAGEMENT(AppInstance appInstance, MongoDBConnect dbConnect) {
        super(appInstance, dbConnect);
    }

    @MessageMongoRecieved(messageType = EMongoState.BEGIN)
    public EMongoState findTransaction() {
        EMongoState nextState = null;
        try {
            String refId = appInstance.getPcefInstance().getRefundManagementRequest().getRefId();
            DBCursor transactionCursor = dbConnect.getTransactionService().findTransactionForRefund(refId);
            if (transactionCursor.hasNext()) {
                Transaction transactionRefund = gson.fromJson(gson.toJson(transactionCursor.next()), Transaction.class);
                appInstance.getPcefInstance().setTransaction(transactionRefund);

                if (EStatusLifeCycle.Done.getName().equals(transactionRefund.getStatus())) {
                    nextState = EMongoState.REFUND_STATUS_DONE;
                } else {
                    //status complete
                    nextState = EMongoState.REFUND_STATUS_COMPLETED;
                }
            } else {
                AFLog.d("Transaction refund not found by ref:" + refId);
                //response error
                nextState = EMongoState.END;
                setSuccess(false);
            }


        } catch (Exception e) {

        }
        return nextState;
    }


    @MessageMongoRecieved(messageType = EMongoState.REFUND_STATUS_DONE)
    public EMongoState refundStatusDone() {
        EMongoState nextState = null;
        try {
            Transaction transactionRefund = appInstance.getPcefInstance().getTransaction();
            DBObject quotaDBObj = dbConnect.getQuotaService().findAndModifyLockQuota(transactionRefund.getMonitoringKey());
            if (quotaDBObj == null) {
                interval.waitInterval();
                nextState = EMongoState.BEGIN;
            } else {
                setPcefState(EState.END);
                nextState = EMongoState.END;
                setSuccess(true);
            }


        } catch (Exception e) {

        }


        return nextState;
    }

    @MessageMongoRecieved(messageType = EMongoState.REFUND_STATUS_COMPLETED)
    public EMongoState refundStatusCompleted() {
        EMongoState nextState = null;
        try {

            Transaction transactionRefund = appInstance.getPcefInstance().getTransaction();
            DBObject quotaDBObj = dbConnect.getQuotaService().findAndModifyLockQuota(transactionRefund.getMonitoringKey());
            if (quotaDBObj == null) {
                interval.waitInterval();
                nextState = EMongoState.BEGIN;
            } else {
                nextState = EMongoState.END;
                setPcefState(EState.W_REFUND_TRANSACTION);
            }

        } catch (Exception e) {

        }


        return nextState;
    }


    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
