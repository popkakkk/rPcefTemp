package phoebe.eqx.pcef.message;

import com.google.gson.Gson;
import ec02.af.abstracts.AbstractAF;
import ec02.af.data.AFDataFactory;
import ec02.af.utils.AFLog;
import ec02.data.interfaces.EquinoxRawData;
import phoebe.eqx.pcef.enums.config.EConfig;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.message.req.UsageMonitroingStartRequest;
import phoebe.eqx.pcef.message.testmsg.TestRequest;
import phoebe.eqx.pcef.utils.PCEFUtils;

import java.util.HashMap;
import java.util.Map;

public class MessagePool {
    //private EquinoxMessageBuilder builder = new EquinoxMessageBuilder();
    private AbstractAF abstractAF;
    private AppInstance appInstance;
    private Object requestObj;


    public MessagePool(AbstractAF abstractAF, AppInstance appInstance) {
        this.abstractAF = abstractAF;
        this.appInstance = appInstance;
    }


    public EquinoxRawData getUsageMonitoringStartRequest(UsageMonitroingStartRequest usageMonitroingStartRequest, String invokeId) {
        String reqData = new Gson().toJson(usageMonitroingStartRequest);
        EquinoxRawData rawData = AFDataFactory.createEquinoxRawData();
        this.requestObj = reqData;
        HashMap<String, String> map = new HashMap<>();
        map.put("name", "HTTP");
        map.put("type", "request");
        map.put("ctype", "text/plain");
        map.put("to", PCEFUtils.getWarmConfig(abstractAF, EConfig.RESOURCE_NAME_TEST));
        map.put("oid", "0.0.17.1218.8.7.0");
        map.put("timeout", PCEFUtils.getWarmConfig(abstractAF, EConfig.TIMEOUT_TEST));
        map.put("invoke", invokeId);

        rawData.setRawDataAttributes(map);
        rawData.setRawDataMessage(reqData);
        return rawData;
    }


    public EquinoxRawData getHTTPTest(String data, String invokeId) {
        AFLog.d("Build ");
        EquinoxRawData rawData = AFDataFactory.createEquinoxRawData();
        TestRequest testRequest = new TestRequest("7", data);
        this.requestObj = testRequest;

        Map<String, String> map = new HashMap();
        map.put("name", "HTTP");
        map.put("type", "request");
        map.put("ctype", "text/plain");
        map.put("to", PCEFUtils.getWarmConfig(abstractAF, EConfig.RESOURCE_NAME_TEST));
        map.put("oid", "0.0.17.1218.8.7.0");
        map.put("timeout", PCEFUtils.getWarmConfig(abstractAF, EConfig.TIMEOUT_TEST));

        map.put("invoke", invokeId);

        rawData.setRawDataAttributes(map);
        rawData.setRawDataMessage(PCEFUtils.gsonToJson(testRequest));
        return rawData;
    }


    public Object getRequestObj() {
        return requestObj;
    }
}
