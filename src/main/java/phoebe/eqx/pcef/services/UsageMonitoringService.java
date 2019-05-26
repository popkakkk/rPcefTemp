package phoebe.eqx.pcef.services;

import ec02.data.interfaces.EquinoxRawData;
import phoebe.eqx.pcef.core.PCEFParser;
import phoebe.eqx.pcef.enums.EStatusResponse;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.message.builder.MessagePool;
import phoebe.eqx.pcef.message.builder.res.UsageMonitoringResponse;
import phoebe.eqx.pcef.message.parser.req.UsageMonitoringRequest;
import phoebe.eqx.pcef.services.PCEFService;
import phoebe.eqx.pcef.utils.MessageFlow;
import phoebe.eqx.pcef.utils.PCEFUtils;

import java.util.Date;

public class UsageMonitoringService extends PCEFService {


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
            usageMonitoringResponse.setCommand("usageMonitoring");
            usageMonitoringResponse.setStatus(EStatusResponse.SUCCESS.getCode());
            usageMonitoringResponse.setDevMessage(EStatusResponse.SUCCESS.getDescription());
            usageMonitoringResponse.setTid(appInstance.getPcefInstance().getTransaction().getTid());
            usageMonitoringResponse.setRtid(appInstance.getPcefInstance().getTransaction().getRtid());
            usageMonitoringResponse.setSessionId(appInstance.getPcefInstance().getTransaction().getSessionId());

            //build message
            MessagePool messagePool = new MessagePool(abstractAF);

            EquinoxRawData equinoxRawData = messagePool.getUsageMonitoringResponse(usageMonitoringResponse, invokeId, getTimeoutFromAppoinmentDate());


            appInstance.getOutList().add(equinoxRawData);
//            appInstance.setFinish(true);

            //increase stat
//            PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.PCEF_RECEIVE_TEST_DATA);


            PCEFUtils.writeMessageFlow("Build Usage Monitoring Response", MessageFlow.Status.Success, appInstance.getPcefInstance().getSessionId());


        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Build Usage Monitoring Response-" + e.getStackTrace()[0], MessageFlow.Status.Error, appInstance.getPcefInstance().getSessionId());
        }


    }

    public void buildResponseUsageMonitoringFail() {


    }


}
