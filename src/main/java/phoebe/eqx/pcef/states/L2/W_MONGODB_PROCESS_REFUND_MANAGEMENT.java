package phoebe.eqx.pcef.states.L2;


import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import ec02.af.utils.AFLog;
import phoebe.eqx.pcef.core.exceptions.TimeoutIntervalException;
import phoebe.eqx.pcef.core.model.Transaction;
import phoebe.eqx.pcef.enums.EStatusLifeCycle;
import phoebe.eqx.pcef.enums.state.EState;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.services.E11TimoutService;
import phoebe.eqx.pcef.services.mogodb.MongoDBConnect;
import phoebe.eqx.pcef.states.abs.MessageRecieved;
import phoebe.eqx.pcef.states.abs.MongoState;

public class W_MONGODB_PROCESS_REFUND_MANAGEMENT extends MongoState {


    public W_MONGODB_PROCESS_REFUND_MANAGEMENT(AppInstance appInstance, MongoDBConnect dbConnect) {
        super(appInstance, Level.L2, dbConnect);
    }

    @MessageRecieved(messageType = EState.BEGIN)
    public void findTransaction() {
        EState nextState = null;
        try {
            String refId = context.getPcefInstance().getRefundManagementRequest().getRefId();
            DBCursor transactionCursor = dbConnect.getTransactionService().findTransactionForRefund(refId);
            if (transactionCursor.hasNext()) {
                Transaction transactionRefund = gson.fromJson(gson.toJson(transactionCursor.next()), Transaction.class);
                context.getPcefInstance().setTransaction(transactionRefund);

                if (EStatusLifeCycle.Done.getName().equals(transactionRefund.getStatus())) {
                    nextState = EState.REFUND_STATUS_DONE;
                } else {
                    //status complete
                    nextState = EState.REFUND_STATUS_COMPLETED;
                }
            } else {
                AFLog.d("Transaction refund not found by ref:" + refId);
                //response error
                setResponseFail();
                nextState = EState.END;
            }


        } catch (Exception e) {
            setResponseFail();
            nextState = EState.END;
        }
        setWorkState(nextState);
    }


    @MessageRecieved(messageType = EState.REFUND_STATUS_DONE)
    public void refundStatusDone() {
        EState nextState = null;
        try {
            Transaction transactionRefund = context.getPcefInstance().getTransaction();
            DBObject quotaDBObj = dbConnect.getQuotaService().findAndModifyLockQuota(transactionRefund.getMonitoringKey());
            if (quotaDBObj == null) {
                E11TimoutService e11TimoutService = new E11TimoutService(appInstance);
                e11TimoutService.buildInterval();
                nextState = EState.BEGIN;
            } else {
                setResponseSuccess();
                nextState = EState.END;
            }


        } catch (TimeoutIntervalException e) {
            setResponseFail();
            nextState = EState.END;
        } catch (Exception e) {
            setResponseFail();
            nextState = EState.END;
        }


        setWorkState(nextState);
    }

    @MessageRecieved(messageType = EState.REFUND_STATUS_COMPLETED)
    public void refundStatusCompleted() {
        EState nextState = null;
        try {

            Transaction transactionRefund = context.getPcefInstance().getTransaction();
            DBObject quotaDBObj = dbConnect.getQuotaService().findAndModifyLockQuota(transactionRefund.getMonitoringKey());
            if (quotaDBObj == null) {
                E11TimoutService e11TimoutService = new E11TimoutService(appInstance);
                e11TimoutService.buildInterval();
                nextState = EState.BEGIN;
            } else {
                nextState = EState.END;
                setState(EState.W_REFUND_TRANSACTION);
            }

        } catch (TimeoutIntervalException e) {
            setResponseFail();
            nextState = EState.END;
        } catch (Exception e) {
            setResponseFail();
            nextState = EState.END;
        }


        setWorkState(nextState);
    }


}
