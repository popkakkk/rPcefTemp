package phoebe.eqx.pcef.services;

import ec02.data.interfaces.EquinoxRawData;
import phoebe.eqx.pcef.core.exceptions.ResponseErrorException;
import phoebe.eqx.pcef.core.exceptions.TimeoutException;
import phoebe.eqx.pcef.core.logs.summary.SummaryLog;
import phoebe.eqx.pcef.enums.Operation;
import phoebe.eqx.pcef.enums.stats.EStatCmd;
import phoebe.eqx.pcef.enums.stats.EStatMode;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.instance.TestResponseData;
import phoebe.eqx.pcef.utils.PCEFUtils;
import phoebe.eqx.pcef.utils.ValidateMessage;

public class SDFService extends PCEFService {

    public SDFService(AppInstance appInstance) {
        super(appInstance);
    }

    public void buildGetResourceId() {
        try {
            //create invokeId
            String invokeId = "getResourceId_" /*+ PCEFUtils.getDate(0).getTime() + PCEFUtils.randomNumber3Digit()*/;

            // logic build
            String data = "test data";

            //build message
            EquinoxRawData equinoxRawData = msgPool.getHTTPTest(data, invokeId);

            //add raw data to list
            invokeExternal(equinoxRawData, Operation.GetResourceId, msgPool.getRequestObj());

            //increase stat
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.PCEF_RECEIVE_TEST_DATA);


        } catch (Exception e) {

        }
    }


    public void readGetResourceId() {
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

        } catch (TimeoutException e) {
            // handle time out
        } catch (ResponseErrorException e) {
            // handle ret error
        } catch (Exception e) {
            //increase stat fail
            //summarylog fail
            // read fail

        }

    }


}
