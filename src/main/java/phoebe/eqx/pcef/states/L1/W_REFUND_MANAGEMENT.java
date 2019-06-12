package phoebe.eqx.pcef.states.L1;

import ec02.af.utils.AFLog;
import phoebe.eqx.pcef.core.model.Transaction;
import phoebe.eqx.pcef.enums.state.EState;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.services.GenerateCDRService;
import phoebe.eqx.pcef.services.RefundManagementService;
import phoebe.eqx.pcef.services.RefundTransactionService;
import phoebe.eqx.pcef.services.mogodb.MongoDBConnect;
import phoebe.eqx.pcef.states.abs.ComplexState;
import phoebe.eqx.pcef.states.abs.MessageRecieved;
import phoebe.eqx.pcef.states.L2.W_MONGODB_PROCESS_REFUND_MANAGEMENT;

public class W_REFUND_MANAGEMENT extends ComplexState {

    public W_REFUND_MANAGEMENT(AppInstance appInstance) {
        super(appInstance, Level.L1);
        this.setState(EState.BEGIN);
    }

    @MessageRecieved(messageType = EState.BEGIN)
    public void begin() throws Exception {
        EState nextState = null;

        RefundManagementService refundManagementService = new RefundManagementService(appInstance);
        refundManagementService.readRefundManagement();

        MongoDBConnect dbConnect = null;
        try {
            dbConnect = new MongoDBConnect(appInstance);
            W_MONGODB_PROCESS_REFUND_MANAGEMENT mongodbProcessRefundManagement = new W_MONGODB_PROCESS_REFUND_MANAGEMENT(appInstance, dbConnect);
            mongodbProcessRefundManagement.dispatch();

            nextState = context.getStateL2();
            if (EState.END.equals(context.getStateL3())) {
                if (EState.END.equals(nextState)) {
                    if (mongodbProcessRefundManagement.isResponseSuccess()) {
                        //refund status done
                        refundSuccess(dbConnect, refundManagementService, GenerateCDRService.STATUS_CANCELED);
                    } else {
                        //refund error
                        refundManagementService.buildResponseRefundManagement(false);
                    }

                } else {
                    if (EState.W_REFUND_TRANSACTION.equals(nextState)) {
                        //refund status complete
                        RefundTransactionService refundTransactionService = new RefundTransactionService(appInstance);
                        refundTransactionService.buildRefundTransactionRequest();
                    }

                }
            }
        } catch (Exception e) {
            AFLog.d(" error:" + e.getStackTrace()[0]);
            throw e;
        } finally {
            if (dbConnect != null) {
                dbConnect.closeConnection();
            }
        }
        setWorkState(nextState);
    }


    @MessageRecieved(messageType = EState.W_REFUND_TRANSACTION)
    public void wRefundTransaction() throws Exception {
        RefundTransactionService refundTransactionService = new RefundTransactionService(appInstance);
        refundTransactionService.readRefundTransactionResponse();

        MongoDBConnect dbConnect = null;
        try {
            dbConnect = new MongoDBConnect(appInstance);

            RefundManagementService refundManagementService = new RefundManagementService(appInstance);
            if (refundTransactionService.isRefundSuccess()) {
                refundSuccess(dbConnect, refundManagementService, GenerateCDRService.STATUS_AlREADY_REFUND);
            } else {
                refundManagementService.buildResponseRefundManagement(false);
            }

        } catch (Exception e) {
            AFLog.d(" wRefundTransaction:" + e.getStackTrace()[0]);
            throw e;
        } finally {
            if (dbConnect != null) {
                dbConnect.closeConnection();
            }

        }
        setWorkState(EState.END);
    }


    private void refundSuccess(MongoDBConnect dbConnect, RefundManagementService refundManagementService, String cdrStatus) {

        Transaction transactionRefund = appInstance.getMyContext().getPcefInstance().getTransaction();

        //delete transaction
        dbConnect.getTransactionService().deleteTransactionByTid(transactionRefund.getTid());

        GenerateCDRService generateCDRService = new GenerateCDRService();
        generateCDRService.buildCDRRefund(cdrStatus, appInstance);

        //update quota unlock
        dbConnect.getQuotaService().updateUnLockQuota(transactionRefund.getMonitoringKey(), context.getPcefInstance().getTransaction().getUserValue());

        refundManagementService.buildResponseRefundManagement(true);


    }

}
