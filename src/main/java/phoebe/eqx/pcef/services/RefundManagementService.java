package phoebe.eqx.pcef.services;

import ec02.data.interfaces.EquinoxRawData;
import phoebe.eqx.pcef.core.PCEFParser;
import phoebe.eqx.pcef.enums.EStatusResponse;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.message.builder.MessagePool;
import phoebe.eqx.pcef.message.builder.res.RefundManagementResponse;
import phoebe.eqx.pcef.message.parser.req.RefundManagementRequest;
import phoebe.eqx.pcef.utils.MessageFlow;
import phoebe.eqx.pcef.utils.PCEFUtils;

public class RefundManagementService extends PCEFService {

    public RefundManagementService(AppInstance appInstance) {
        super(appInstance);
    }


    public void readRefundManagement() {
        try {
            PCEFParser pcefParser = new PCEFParser(appInstance.getReqMessage());
            RefundManagementRequest refundManagementRequest = pcefParser.translateRefundManagementRequest();
            appInstance.getPcefInstance().setRefundManagementRequest(refundManagementRequest);
            appInstance.getPcefInstance().setSessionId(refundManagementRequest.getSessionId());

            PCEFUtils.writeMessageFlow("Read Refund Management Request", MessageFlow.Status.Success, appInstance.getPcefInstance().getSessionId());
        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Read Refund Management Request-" + e.getStackTrace()[0], MessageFlow.Status.Success, appInstance.getPcefInstance().getSessionId());
        }


    }

    public void buildResponseRefundManagement(boolean success) {

        try {
            //create invokeId
            String invokeId = appInstance.getRequestInvokeId();

            // logic build
            RefundManagementResponse refundManagementResponse = new RefundManagementResponse();
            refundManagementResponse.setCommand("refundManagement");
            refundManagementResponse.setSessionId(appInstance.getPcefInstance().getRefundManagementRequest().getSessionId());

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
            EquinoxRawData equinoxRawData = messagePool.getRefundManagementResponse(refundManagementResponse, invokeId, getTimeoutFromAppoinmentDate());

            appInstance.getOutList().add(equinoxRawData);
//            appInstance.setFinish(true);

            PCEFUtils.writeMessageFlow("Build Refund Management Response", MessageFlow.Status.Success, appInstance.getPcefInstance().getSessionId());
        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Build Refund Management Response -" + e.getStackTrace()[0], MessageFlow.Status.Error, appInstance.getPcefInstance().getSessionId());
        }


    }


}
