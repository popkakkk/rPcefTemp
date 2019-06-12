package phoebe.eqx.pcef.services;

import ec02.af.utils.AFLog;
import ec02.data.interfaces.EquinoxRawData;
import org.apache.commons.lang3.exception.ExceptionUtils;
import phoebe.eqx.pcef.core.exceptions.PCEFException;
import phoebe.eqx.pcef.core.exceptions.ResponseErrorException;
import phoebe.eqx.pcef.core.exceptions.TimeoutException;
import phoebe.eqx.pcef.core.model.Transaction;
import phoebe.eqx.pcef.enums.EError;
import phoebe.eqx.pcef.enums.EStatusResponse;
import phoebe.eqx.pcef.enums.Operation;
import phoebe.eqx.pcef.enums.stats.EStatCmd;
import phoebe.eqx.pcef.enums.stats.EStatMode;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.message.builder.MessagePool;
import phoebe.eqx.pcef.message.builder.req.RefundTransactionRequest;
import phoebe.eqx.pcef.message.builder.res.RefundManagementResponse;
import phoebe.eqx.pcef.message.parser.res.RefundTransactionResponse;
import phoebe.eqx.pcef.utils.MessageFlow;
import phoebe.eqx.pcef.utils.PCEFUtils;
import phoebe.eqx.pcef.utils.ValidateMessage;
import phoebe.eqx.pcef.utils.WriteLog;

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

            PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.sent_Refund_Transaction_request);
        } catch (Exception e) {
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.ERROR, EStatCmd.sent_Refund_Transaction_request);
            PCEFException pcefException = new PCEFException();
            pcefException.setErrorMsg(ExceptionUtils.getStackTrace(e));
            pcefException.setError(EError.REFUND_TRANSACTION_BUILD_REQUEST_ERROR);

            context.setPcefException(pcefException);
            throw e;
        }
    }


    public void readRefundTransactionResponse() throws Exception {
        try {
            try {
                AFLog.d("Read Refund Transaction Response ..");
                Operation operation = Operation.RefundTransaction;

                RefundTransactionResponse refundManagementResponse = (RefundTransactionResponse) extractResponse(operation);
                this.refundSuccess = refundManagementResponse.getStatus().equals(EStatusResponse.SUCCESS.getCode());

                ValidateMessage.validateRefundTransaction(refundManagementResponse, abstractAF);

                //summarylog res
//            context.setSummaryLogExternalResponse(Operation.TestOperation, SummaryLog.getSummaryLogResponse(Operation.TestOperation, testResponseData));

                PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.receive_Refund_Transaction_response);
            } catch (TimeoutException e) {
                PCEFUtils.increaseStatistic(abstractAF, EStatMode.TIMEOUT, EStatCmd.receive_Refund_Transaction_response);
                e.setError(EError.REFUND_TRANSACTION_RESPONSE_TIMEOUT);
                throw e;
            } catch (ResponseErrorException e) {
                PCEFUtils.increaseStatistic(abstractAF, EStatMode.EQUINOX_ERROR, EStatCmd.receive_Refund_Transaction_response);
                e.setError(EError.REFUND_TRANSACTION_RESPONSE_EQUINOX_ERROR);
                throw e;
            }
        } catch (PCEFException e) {
            //summarylog fail
            context.setPcefException(e);
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
