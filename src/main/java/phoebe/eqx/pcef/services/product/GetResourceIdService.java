package phoebe.eqx.pcef.services.product;

import ec02.data.interfaces.EquinoxRawData;
import phoebe.eqx.pcef.core.exceptions.ResponseErrorException;
import phoebe.eqx.pcef.core.exceptions.TimeoutException;
import phoebe.eqx.pcef.enums.Operation;
import phoebe.eqx.pcef.enums.stats.EStatCmd;
import phoebe.eqx.pcef.enums.stats.EStatMode;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.message.builder.MessagePool;
import phoebe.eqx.pcef.message.builder.req.GetResourceIdRequest;
import phoebe.eqx.pcef.services.PCEFService;
import phoebe.eqx.pcef.utils.MessageFlow;
import phoebe.eqx.pcef.utils.PCEFUtils;
import phoebe.eqx.pcef.utils.ValidateMessage;

public class GetResourceIdService extends PCEFService {

    public GetResourceIdService(AppInstance appInstance) {
        super(appInstance);
    }

    public void buildGetResourceId() {
        try {
            //create invokeId
            String invokeId = "getResourceId_" /*+ PCEFUtils.getDate(0).getTime() + PCEFUtils.randomNumber3Digit()*/;

            // logic build
            String data = "test data";

            GetResourceIdRequest getResourceIdRequest = new GetResourceIdRequest();

            //build message
            MessagePool messagePool = new MessagePool(abstractAF);
            EquinoxRawData equinoxRawData = messagePool.getResourceIdRequest(getResourceIdRequest, invokeId);

            //add raw data to list
            invokeExternal(equinoxRawData, Operation.GetResourceId, messagePool.getRequestObj());

            //increase stat
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.PCEF_RECEIVE_TEST_DATA);

            PCEFUtils.writeMessageFlow("Build Get Resource ID Request", MessageFlow.Status.Success, appInstance.getPcefInstance().getSessionId());
        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Build Get Resource ID Request", MessageFlow.Status.Error, appInstance.getPcefInstance().getSessionId());
        }
    }


    public String readGetResourceId() {
        String resourceId = null;
        try {
            //extract
            Operation operation = Operation.GetResourceId;

            /*TestResponseData testResponseData = (TestResponseData) extractResponse(operation);
            this.appInstance.getPcefInstance().setTestResponseData(testResponseData);*/

            //validate
            ValidateMessage.validateTestData();

            //increase stat success
//            PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.PCEF_RECEIVE_TEST_DATA);

            //summarylog res
//            appInstance.setSummaryLogExternalResponse(operation, SummaryLog.getSummaryLogResponse(operation, testResponseData));

            resourceId = "resourceId1234";

            PCEFUtils.writeMessageFlow("Build Get Resource ID Request", MessageFlow.Status.Success, appInstance.getPcefInstance().getSessionId());
        } catch (TimeoutException e) {
            // handle time out
        } catch (ResponseErrorException e) {
            // handle ret error
        } catch (Exception e) {
            //increase stat fail
            //summarylog fail
            // read fail
            PCEFUtils.writeMessageFlow("Build Get Resource ID Request", MessageFlow.Status.Error, appInstance.getPcefInstance().getSessionId());

        }

        return resourceId;
    }


}
