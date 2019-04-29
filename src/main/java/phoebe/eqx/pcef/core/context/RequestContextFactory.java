package phoebe.eqx.pcef.core.context;

import phoebe.eqx.pcef.enums.ERequestType;
import phoebe.eqx.pcef.instance.AppInstance;

import java.util.Date;

public class RequestContextFactory {

    public static void getRequestTest(String reqMessage, AppInstance appInstance) {
        appInstance.create();
        appInstance.getRequestContext().setStartTime(new Date());
        appInstance.getRequestContext().setReqMessage(reqMessage);
        appInstance.getRequestContext().setRequestType(ERequestType.TEST_CMD);
    }


    public static void getUsageMonitoring(String reqMessage, AppInstance appInstance) {
        appInstance.create();
        appInstance.getRequestContext().setStartTime(new Date());
        appInstance.getRequestContext().setReqMessage(reqMessage);
        appInstance.getRequestContext().setRequestType(ERequestType.USAGE_MONITORING);
    }


}
