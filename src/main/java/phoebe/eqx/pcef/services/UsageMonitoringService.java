package phoebe.eqx.pcef.services;

import ec02.af.utils.AFLog;
import ec02.data.interfaces.EquinoxRawData;
import phoebe.eqx.pcef.core.PCEFParser;
import phoebe.eqx.pcef.core.exceptions.ExtractErrorException;
import phoebe.eqx.pcef.core.exceptions.MissingParameterException;
import phoebe.eqx.pcef.core.exceptions.PCEFException;
import phoebe.eqx.pcef.core.exceptions.WrongFormatException;
import phoebe.eqx.pcef.enums.EError;
import phoebe.eqx.pcef.enums.EStatusResponse;
import phoebe.eqx.pcef.enums.state.EState;
import phoebe.eqx.pcef.enums.stats.EStatCmd;
import phoebe.eqx.pcef.enums.stats.EStatMode;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.message.builder.MessagePool;
import phoebe.eqx.pcef.message.builder.res.UsageMonitoringResponse;
import phoebe.eqx.pcef.message.parser.req.UsageMonitoringRequest;
import phoebe.eqx.pcef.utils.MessageFlow;
import phoebe.eqx.pcef.utils.PCEFUtils;
import phoebe.eqx.pcef.utils.ValidateMessage;
import phoebe.eqx.pcef.utils.WriteLog;

public class UsageMonitoringService extends PCEFService {


    public UsageMonitoringService(AppInstance appInstance) {
        super(appInstance);
    }

    public void readUsageMonitoringRequest() throws PCEFException {
        try {
            AFLog.d("Read Usage Monitoring Request ..");

            PCEFParser pcefParser = new PCEFParser(context.getReqMessage());
            UsageMonitoringRequest usageMonitoringRequest = pcefParser.translateUsageMonitoringRequest();
            context.getPcefInstance().setUsageMonitoringRequest(usageMonitoringRequest);
            context.getPcefInstance().setSessionId(usageMonitoringRequest.getSessionId());

            ValidateMessage.validateUsageMonitoringRequest(usageMonitoringRequest);

            PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.Receive_Usage_Monitoring_Request);
            PCEFUtils.writeMessageFlow("Read Usage Monitoring Request", MessageFlow.Status.Success, context.getPcefInstance().getSessionId());

        } catch (PCEFException e) {
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.ERROR, EStatCmd.Receive_Usage_Monitoring_Request);
            PCEFUtils.writeMessageFlow("Read Usage Monitoring Request", MessageFlow.Status.Error, context.getPcefInstance().getSessionId());
            WriteLog.writeErrorLogUsageMonitoring(abstractAF, e, context.getPcefInstance().getUsageMonitoringRequest(), "");
            throw e;
        }

    }


    public void buildResponseUsageMonitoring(boolean success) {
        try {
            AFLog.d("Build Usage Monitoring Response ..");

            String invokeId = context.getRequestInvokeId();

            UsageMonitoringResponse usageMonitoringResponse = new UsageMonitoringResponse();
            usageMonitoringResponse.setCommand("usageMonitoring");

            usageMonitoringResponse.setTid(context.getPcefInstance().getTransaction().getTid());
            usageMonitoringResponse.setRtid(context.getPcefInstance().getTransaction().getRtid());
            usageMonitoringResponse.setSessionId(context.getPcefInstance().getTransaction().getSessionId());
            if (success) {
                AFLog.d("Build Usage Monitoring Response Success ..");
                usageMonitoringResponse.setStatus(EStatusResponse.SUCCESS.getCode());
                usageMonitoringResponse.setDevMessage(EStatusResponse.SUCCESS.getDescription());
                PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.Sent_Usage_Monitoring_Response);
            } else {
                AFLog.d("Build Usage Monitoring Response Fail ..");
                usageMonitoringResponse.setStatus(EStatusResponse.FAIL.getCode());
                usageMonitoringResponse.setDevMessage(EStatusResponse.FAIL.getDescription());
                PCEFUtils.increaseStatistic(abstractAF, EStatMode.ERROR, EStatCmd.Sent_Usage_Monitoring_Response);
            }

            //build message
            MessagePool messagePool = new MessagePool(abstractAF);
            EquinoxRawData equinoxRawData = messagePool.getUsageMonitoringResponse(usageMonitoringResponse, invokeId, getTimeoutFromAppointmentDate());
            appInstance.getOutList().add(equinoxRawData);

            PCEFUtils.writeMessageFlow("Build Usage Monitoring Response", MessageFlow.Status.Success, context.getPcefInstance().getSessionId());
        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Build Usage Monitoring Response-" + e.getStackTrace()[0], MessageFlow.Status.Error, context.getPcefInstance().getSessionId());
            throw e;
        }


    }


}
