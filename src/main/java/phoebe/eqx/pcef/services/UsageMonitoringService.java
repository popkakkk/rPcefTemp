package phoebe.eqx.pcef.services;

import ec02.af.utils.AFLog;
import ec02.data.interfaces.EquinoxRawData;
import phoebe.eqx.pcef.core.PCEFParser;
import phoebe.eqx.pcef.enums.EStatusResponse;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.message.builder.MessagePool;
import phoebe.eqx.pcef.message.builder.res.UsageMonitoringResponse;
import phoebe.eqx.pcef.message.parser.req.UsageMonitoringRequest;
import phoebe.eqx.pcef.utils.MessageFlow;
import phoebe.eqx.pcef.utils.PCEFUtils;

public class UsageMonitoringService extends PCEFService {


    public UsageMonitoringService(AppInstance appInstance) {
        super(appInstance);

    }

    public void readUsageMonitoringRequest() {
        try {
            AFLog.d("Read Usage Monitoring Request ..");

            PCEFParser pcefParser = new PCEFParser(context.getReqMessage());
            UsageMonitoringRequest usageMonitoringRequest = pcefParser.translateUsageMonitoringRequest();
            context.getPcefInstance().setUsageMonitoringRequest(usageMonitoringRequest);
            context.getPcefInstance().setSessionId(usageMonitoringRequest.getSessionId());

            PCEFUtils.writeMessageFlow("Read Usage Monitoring Request", MessageFlow.Status.Success, context.getPcefInstance().getSessionId());
        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Read Usage Monitoring Request", MessageFlow.Status.Error, context.getPcefInstance().getSessionId());
        }


    }


    public void buildResponseUsageMonitoringSuccess() {
        try {
            AFLog.d("Build Usage Monitoring Response ..");

            //create invokeId
            String invokeId = context.getRequestInvokeId();

            // logic build
            UsageMonitoringResponse usageMonitoringResponse = new UsageMonitoringResponse();
            usageMonitoringResponse.setCommand("usageMonitoring");
            usageMonitoringResponse.setStatus(EStatusResponse.SUCCESS.getCode());
            usageMonitoringResponse.setDevMessage(EStatusResponse.SUCCESS.getDescription());
            usageMonitoringResponse.setTid(context.getPcefInstance().getTransaction().getTid());
            usageMonitoringResponse.setRtid(context.getPcefInstance().getTransaction().getRtid());
            usageMonitoringResponse.setSessionId(context.getPcefInstance().getTransaction().getSessionId());

            //build message
            MessagePool messagePool = new MessagePool(abstractAF);

            EquinoxRawData equinoxRawData = messagePool.getUsageMonitoringResponse(usageMonitoringResponse, invokeId, getTimeoutFromAppointmentDate());


            appInstance.getOutList().add(equinoxRawData);
//            context.setFinish(true);

            //increase stat
//            PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.PCEF_RECEIVE_TEST_DATA);


            PCEFUtils.writeMessageFlow("Build Usage Monitoring Response", MessageFlow.Status.Success, context.getPcefInstance().getSessionId());


        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Build Usage Monitoring Response-" + e.getStackTrace()[0], MessageFlow.Status.Error, context.getPcefInstance().getSessionId());
        }


    }

    public void buildResponseUsageMonitoringFail() {


    }


}
