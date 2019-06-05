package phoebe.eqx.pcef.services;

import ec02.af.utils.AFLog;
import ec02.data.interfaces.EquinoxRawData;
import org.apache.commons.lang3.exception.ExceptionUtils;
import phoebe.eqx.pcef.core.PCEFParser;
import phoebe.eqx.pcef.core.exceptions.PCEFException;
import phoebe.eqx.pcef.enums.EError;
import phoebe.eqx.pcef.enums.EStatusResponse;
import phoebe.eqx.pcef.enums.stats.EStatCmd;
import phoebe.eqx.pcef.enums.stats.EStatMode;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.message.builder.MessagePool;
import phoebe.eqx.pcef.message.builder.res.RefundManagementResponse;
import phoebe.eqx.pcef.message.parser.req.RefundManagementRequest;
import phoebe.eqx.pcef.utils.MessageFlow;
import phoebe.eqx.pcef.utils.PCEFUtils;
import phoebe.eqx.pcef.utils.ValidateMessage;
import phoebe.eqx.pcef.utils.WriteLog;

public class RefundManagementService extends PCEFService {

    public RefundManagementService(AppInstance appInstance) {
        super(appInstance);
    }


    public void readRefundManagement() {
        try {
            AFLog.d("Read Refund Management Request..");

            PCEFParser pcefParser = new PCEFParser(context.getReqMessage());
            RefundManagementRequest refundManagementRequest = pcefParser.translateRefundManagementRequest();
            context.getPcefInstance().setRefundManagementRequest(refundManagementRequest);
            context.getPcefInstance().setSessionId(refundManagementRequest.getSessionId());

            ValidateMessage.validateRefundManagement(refundManagementRequest, abstractAF);

            PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.Receive_Refund_Management_Request);
            PCEFUtils.writeMessageFlow("Read Refund Management Request", MessageFlow.Status.Success, context.getPcefInstance().getSessionId());
        } catch (PCEFException e) {
            context.setPcefException(e);
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.ERROR, EStatCmd.Receive_Refund_Management_Request);
            PCEFUtils.writeMessageFlow("Read Refund Management Request-" + e.getStackTrace()[0], MessageFlow.Status.Success, context.getPcefInstance().getSessionId());
        }


    }

    public void buildResponseRefundManagement(boolean success) {

        try {
            AFLog.d("Build Refund Management Response..");

            //create invokeId
            String invokeId = context.getRequestInvokeId();

            // logic build
            RefundManagementResponse refundManagementResponse = new RefundManagementResponse();
            refundManagementResponse.setCommand("refundManagement");
            refundManagementResponse.setSessionId(context.getPcefInstance().getRefundManagementRequest().getSessionId());
            refundManagementResponse.setRtid(context.getPcefInstance().getRefundManagementRequest().getRtid());
            refundManagementResponse.setTid(context.getPcefInstance().getRefundManagementRequest().getTid());

            if (success) {
                refundManagementResponse.setStatus(EStatusResponse.SUCCESS.getCode());
                refundManagementResponse.setDevMessage(EStatusResponse.SUCCESS.getDescription());
            } else {
                refundManagementResponse.setStatus(EStatusResponse.FAIL.getCode());
                refundManagementResponse.setDevMessage(EStatusResponse.FAIL.getDescription());

            }

            //increase stat
//            PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.PCEF_RECEIVE_TEST_DATA);


            //build message
            MessagePool messagePool = new MessagePool(abstractAF);
            EquinoxRawData equinoxRawData = messagePool.getRefundManagementResponse(refundManagementResponse, invokeId, "10");

            appInstance.getOutList().add(equinoxRawData);
            appInstance.setFinish(true);

            PCEFUtils.writeMessageFlow("Build Refund Management Response", MessageFlow.Status.Success, context.getPcefInstance().getSessionId());
        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Build Refund Management Response -" + e.getStackTrace()[0], MessageFlow.Status.Error, context.getPcefInstance().getSessionId());
       /*     PCEFException pcefException = new PCEFException();
            pcefException.setError(EError.);
            pcefException.setErrorMsg(ExceptionUtils.getStackTrace(e));
            WriteLog.writeErrorLogRefundManagement(abstractAF, pcefException, context.getPcefInstance().getRefundManagementRequest());
       */



        }


    }


}
