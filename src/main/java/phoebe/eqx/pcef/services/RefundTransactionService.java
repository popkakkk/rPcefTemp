package phoebe.eqx.pcef.services;

import ec02.af.utils.AFLog;
import ec02.data.interfaces.EquinoxRawData;
import phoebe.eqx.pcef.core.exceptions.ResponseErrorException;
import phoebe.eqx.pcef.core.exceptions.TimeoutException;
import phoebe.eqx.pcef.core.model.Transaction;
import phoebe.eqx.pcef.enums.EStatusResponse;
import phoebe.eqx.pcef.enums.Operation;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.message.builder.MessagePool;
import phoebe.eqx.pcef.message.builder.req.RefundTransactionRequest;
import phoebe.eqx.pcef.message.builder.res.RefundManagementResponse;
import phoebe.eqx.pcef.utils.MessageFlow;
import phoebe.eqx.pcef.utils.PCEFUtils;
import phoebe.eqx.pcef.utils.ValidateMessage;

import java.util.Date;

public class RefundTransactionService extends PCEFService {
    public RefundTransactionService(AppInstance appInstance) {
        super(appInstance);
    }

    private boolean refundSuccess;

    public void buildRefundTransactionRequest() {
        try {
            AFLog.d("Build Refund Transaction Request..");

            Transaction transactionRefund = context.getPcefInstance().getTransaction();
            Operation operation = Operation.RefundTransaction;
            String invokeId = generateInvokeId(operation);

            RefundTransactionRequest refundTransactionRequest = new RefundTransactionRequest();

            refundTransactionRequest.setCommand("refundTransaction");
            refundTransactionRequest.setUserType(transactionRefund.getUserType());
            refundTransactionRequest.setUserValue(transactionRefund.getUserValue());
            refundTransactionRequest.setActualTime(PCEFUtils.actualTimeDFM.format(new Date()));
            refundTransactionRequest.setResourceId(transactionRefund.getResourceId());
            refundTransactionRequest.setResouceName(transactionRefund.getResourceName());
            refundTransactionRequest.setTid(transactionRefund.getTid());
            refundTransactionRequest.setRtid(transactionRefund.getRtid());
            refundTransactionRequest.setMonitoringKey(transactionRefund.getMonitoringKey());
            refundTransactionRequest.setCounterId(transactionRefund.getCounterId());
            refundTransactionRequest.setUnitType("unit");
            refundTransactionRequest.setUsedUnit("1");
            refundTransactionRequest.setReportingReason("0");
            MessagePool messagePool = new MessagePool(abstractAF);
            EquinoxRawData equinoxRawData = messagePool.getRefundTransactionRequest(refundTransactionRequest, invokeId);
            invokeExternal(equinoxRawData, operation, messagePool.getRequestObj());


            PCEFUtils.writeMessageFlow("Build Refund Transaction Request", MessageFlow.Status.Success, context.getPcefInstance().getSessionId());
        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Build Refund Transaction Stop Request", MessageFlow.Status.Error, context.getPcefInstance().getSessionId());
        }
    }


    public void readRefundTransactionResponse() throws Exception {
        try {
            AFLog.d("Read Refund Transaction Response ..");
            Operation operation = Operation.RefundTransaction;

            //extract
            RefundManagementResponse refundManagementResponse = (RefundManagementResponse) extractResponse(operation);
            this.refundSuccess = refundManagementResponse.getStatus().equals(EStatusResponse.SUCCESS.getCode());

            //validate
            ValidateMessage.validateTestData();


            //increase stat success
//            PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.PCEF_RECEIVE_TEST_DATA);

            //summarylog res
//            context.setSummaryLogExternalResponse(Operation.TestOperation, SummaryLog.getSummaryLogResponse(Operation.TestOperation, testResponseData));

            PCEFUtils.writeMessageFlow("Read Refund Transaction Response", MessageFlow.Status.Success, context.getPcefInstance().getSessionId());
        } catch (TimeoutException e) {
            // handle time out
            throw e;
        } catch (ResponseErrorException e) {

            // handle ret error
            throw e;
        } catch (Exception e) {
            //increase stat fail
            //summarylog fail
            // read fail


            PCEFUtils.writeMessageFlow("Read Refund Transaction Response", MessageFlow.Status.Error, context.getPcefInstance().getSessionId());
            throw e;
        }


    }

    public boolean isRefundSuccess() {
        return refundSuccess;
    }

    public void setRefundSuccess(boolean refundSuccess) {
        this.refundSuccess = refundSuccess;
    }
}
