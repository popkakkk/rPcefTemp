package phoebe.eqx.pcef.services;

import ec02.data.interfaces.EquinoxRawData;
import phoebe.eqx.pcef.core.PCEFParser;
import phoebe.eqx.pcef.enums.EStatusResponse;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.message.builder.MessagePool;
import phoebe.eqx.pcef.message.builder.res.GyRARResponse;
import phoebe.eqx.pcef.message.parser.req.GyRARRequest;
import phoebe.eqx.pcef.utils.MessageFlow;
import phoebe.eqx.pcef.utils.PCEFUtils;

public class GyRARService extends PCEFService {
    public GyRARService(AppInstance appInstance) {
        super(appInstance);
    }


    public void readGyRAR() {
        try {
            PCEFParser pcefParser = new PCEFParser(appInstance.getReqMessage());
            GyRARRequest gyRARRequest = pcefParser.translateGyRARRequest();
            appInstance.getPcefInstance().setGyRARRequest(gyRARRequest);
            appInstance.getPcefInstance().setSessionId(gyRARRequest.getSessionId());

            PCEFUtils.writeMessageFlow("Read GyRAR Request", MessageFlow.Status.Success, appInstance.getPcefInstance().getSessionId());
        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Read GyRAR Request-" + e.getStackTrace()[0], MessageFlow.Status.Success, appInstance.getPcefInstance().getSessionId());
        }


    }

    public void buildResponseGyRAR() {

        try {
            //create invokeId
            String invokeId = appInstance.getRequestInvokeId();

            // logic build
            GyRARResponse gyRARResponse = new GyRARResponse();
            gyRARResponse.setCommand("GyRAR");
            gyRARResponse.setSessionId(appInstance.getPcefInstance().getGyRARRequest().getSessionId());
            gyRARResponse.setStatus(EStatusResponse.SUCCESS.getCode());
            gyRARResponse.setDevMessage(EStatusResponse.SUCCESS.getDescription());

            //build message
            MessagePool messagePool = new MessagePool(abstractAF);
            EquinoxRawData equinoxRawData = messagePool.getGyRARResponse(gyRARResponse, invokeId);

            appInstance.getOutList().add(equinoxRawData);
//            appInstance.setFinish(true);


            appInstance.getOutList().add(equinoxRawData);
//            appInstance.setFinish(true);

            //increase stat
//            PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.PCEF_RECEIVE_TEST_DATA);


            PCEFUtils.writeMessageFlow("Build GyRAR Response", MessageFlow.Status.Success, appInstance.getPcefInstance().getSessionId());
        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Build GyRAR Response -" + e.getStackTrace()[0], MessageFlow.Status.Error, appInstance.getPcefInstance().getSessionId());
        }


    }
}
