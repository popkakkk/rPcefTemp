package phoebe.eqx.pcef.core.context;

import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.states.L1.W_USAGE_MONITORING;

public class PCEFDispatcher {

  /*  public static void getTest(AppInstance appInstance) {
        W_Test w_test = new W_Test(appInstance);
        w_test.dispatch();
        appInstance.patchResponse();
    }*/

/*    public static void getTestMyCMD(AppInstance appInstance) {
        W_TEST_MY_CMD w = new W_TEST_MY_CMD(appInstance);
        w.dispatch();
        appInstance.patchResponse();
    }*/


    public static void dispatchUsageMonitoring(AppInstance appInstance) {
        W_USAGE_MONITORING w = new W_USAGE_MONITORING(appInstance);
        w.dispatch();
        appInstance.patchResponse();
    }


}
