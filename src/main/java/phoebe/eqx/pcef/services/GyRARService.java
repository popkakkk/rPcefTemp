package phoebe.eqx.pcef.services;

import ec02.af.utils.AFLog;
import ec02.data.interfaces.EquinoxRawData;
import phoebe.eqx.pcef.core.PCEFParser;
import phoebe.eqx.pcef.core.exceptions.ExtractErrorException;
import phoebe.eqx.pcef.core.exceptions.MissingParameterException;
import phoebe.eqx.pcef.core.exceptions.PCEFException;
import phoebe.eqx.pcef.core.exceptions.WrongFormatException;
import phoebe.eqx.pcef.enums.EStatusResponse;
import phoebe.eqx.pcef.enums.stats.EStatCmd;
import phoebe.eqx.pcef.enums.stats.EStatMode;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.message.builder.MessagePool;
import phoebe.eqx.pcef.message.builder.res.GyRARResponse;
import phoebe.eqx.pcef.message.parser.req.GyRARRequest;
import phoebe.eqx.pcef.utils.MessageFlow;
import phoebe.eqx.pcef.utils.PCEFUtils;
import phoebe.eqx.pcef.utils.ValidateMessage;

public class GyRARService extends PCEFService {
    public GyRARService(AppInstance appInstance) {
        super(appInstance);
    }


    public void readGyRAR() throws ExtractErrorException, MissingParameterException, WrongFormatException {
        try {
            AFLog.d("Read GyRAR Request..");

            PCEFParser pcefParser = new PCEFParser(context.getReqMessage());
            GyRARRequest gyRARRequest = pcefParser.translateGyRARRequest();
            context.getPcefInstance().setGyRARRequest(gyRARRequest);
            context.getPcefInstance().setSessionId(gyRARRequest.getSessionId());

            ValidateMessage.validateGyRAR(gyRARRequest, abstractAF);

            PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.receive_GyRAR_request);
            PCEFUtils.writeMessageFlow("Read GyRAR Request", MessageFlow.Status.Success, context.getPcefInstance().getSessionId());
        } catch (PCEFException e) {
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.ERROR, EStatCmd.receive_GyRAR_request);
            context.setPcefException(e);
            PCEFUtils.writeMessageFlow("Read GyRAR Request-" + e.getStackTrace()[0], MessageFlow.Status.Success, context.getPcefInstance().getSessionId());
            throw e;
        }


    }

    public void buildResponseGyRAR(boolean success) {

        try {
            AFLog.d("Build GyRAR Response..");

            //create invokeId
            String invokeId = context.getRequestInvokeId();

            // logic build
            GyRARResponse gyRARResponse = new GyRARResponse();
            gyRARResponse.setCommand("GyRAR");
            gyRARResponse.setSessionId(context.getPcefInstance().getGyRARRequest().getSessionId());

            if (success) {
                gyRARResponse.setStatus(EStatusResponse.SUCCESS.getCode());
                gyRARResponse.setDevMessage(EStatusResponse.SUCCESS.getDescription());
                PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.sent_GyRAR_response);
            } else {
                gyRARResponse.setStatus(EStatusResponse.FAIL.getCode());
                gyRARResponse.setDevMessage(EStatusResponse.FAIL.getDescription());
                PCEFUtils.increaseStatistic(abstractAF, EStatMode.ERROR, EStatCmd.sent_GyRAR_response);
                context.setTerminate(true);
            }


            //build message
            MessagePool messagePool = new MessagePool(abstractAF);
            EquinoxRawData equinoxRawData = messagePool.getGyRARResponse(gyRARResponse, invokeId, getTimeoutFromAppointmentDate());
            appInstance.getOutList().add(equinoxRawData);

            //sum log

            PCEFUtils.writeMessageFlow("Build GyRAR Response", MessageFlow.Status.Success, context.getPcefInstance().getSessionId());
        } catch (Exception e) {
           /*
            PCEFException pcefException = new PCEFException();
            pcefException.setError(EError);
            pcefException.setErrorMsg(ExceptionUtils.getStackTrace(e));
            WriteLog.writeErrorLogGyRAR(abstractAF, e, context.getPcefInstance().getGyRARRequest());
*/

            PCEFUtils.writeMessageFlow("Build GyRAR Response -" + e.getStackTrace()[0], MessageFlow.Status.Error, context.getPcefInstance().getSessionId());
            throw e;
        }


    }
}
