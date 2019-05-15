package phoebe.eqx.pcef.services.sacf;

import ec02.data.interfaces.EquinoxRawData;
import phoebe.eqx.pcef.core.PCEFParser;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.message.builder.MessagePool;
import phoebe.eqx.pcef.message.builder.res.UsageMonitoringResponse;
import phoebe.eqx.pcef.message.parser.req.UsageMonitoringRequest;
import phoebe.eqx.pcef.services.PCEFService;
import phoebe.eqx.pcef.utils.MessageFlow;
import phoebe.eqx.pcef.utils.PCEFUtils;

public class UsageMonitoringService extends PCEFService {

    private enum Status {

        SUCCESS("200", "Success"),
        FAIL("500", "Error");
        private String code;
        private String description;

        Status(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }


    public UsageMonitoringService(AppInstance appInstance) {
        super(appInstance);

    }

    public void readUsageMonitoringRequest() {
        try {
            PCEFParser pcefParser = new PCEFParser(appInstance.getReqMessage());
            UsageMonitoringRequest usageMonitoringRequest = pcefParser.translateUsageMonitoringRequest();
            appInstance.getPcefInstance().setUsageMonitoringRequest(usageMonitoringRequest);
            appInstance.getPcefInstance().setSessionId(usageMonitoringRequest.getSessionId());

            PCEFUtils.writeMessageFlow("Read Usage Monitoring Request", MessageFlow.Status.Success, appInstance.getPcefInstance().getSessionId());
        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Read Usage Monitoring Request", MessageFlow.Status.Error, appInstance.getPcefInstance().getSessionId());
        }


    }


    public void buildResponseUsageMonitoringSuccess() {

        try {
            //create invokeId
            String invokeId = appInstance.getRequestInvokeId();

            // logic build
            UsageMonitoringResponse usageMonitoringResponse = new UsageMonitoringResponse();
            usageMonitoringResponse.setStatus(Status.SUCCESS.getCode());
            usageMonitoringResponse.setDevMessage(Status.SUCCESS.getDescription());

            //build message
            MessagePool messagePool = new MessagePool(abstractAF);
            EquinoxRawData equinoxRawData = messagePool.getUsageMonitoringResponse(usageMonitoringResponse, invokeId);
            appInstance.getOutList().add(equinoxRawData);
            appInstance.setFinish(true);

            //increase stat
//            PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.PCEF_RECEIVE_TEST_DATA);


        } catch (Exception e) {

        }


    }

    public void buildResponseUsageMonitoringFail() {


    }


}
