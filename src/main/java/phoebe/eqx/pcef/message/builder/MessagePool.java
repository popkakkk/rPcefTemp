package phoebe.eqx.pcef.message.builder;

import com.google.gson.Gson;
import ec02.af.abstracts.AbstractAF;
import ec02.af.data.AFDataFactory;
import ec02.af.utils.AFLog;
import ec02.data.interfaces.EquinoxRawData;
import phoebe.eqx.pcef.enums.config.EConfig;
import phoebe.eqx.pcef.message.builder.req.GetResourceIdRequest;
import phoebe.eqx.pcef.message.builder.req.OCFUsageMonitoringRequest;
import phoebe.eqx.pcef.message.builder.res.UsageMonitoringResponse;
import phoebe.eqx.pcef.utils.PCEFUtils;

import java.util.HashMap;
import java.util.Map;

public class MessagePool {
    //private EquinoxMessageBuilder builder = new EquinoxMessageBuilder();
    private AbstractAF abstractAF;

    private Object requestObj;


    public MessagePool(AbstractAF abstractAF) {
        this.abstractAF = abstractAF;
    }


    public EquinoxRawData getResourceIdRequest(GetResourceIdRequest getResourceIdRequest, String invokeId) {
        String reqData = new Gson().toJson(getResourceIdRequest);
        EquinoxRawData rawData = AFDataFactory.createEquinoxRawData();
        this.requestObj = reqData;
        Map<String, String> map = new HashMap<>();
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


    public EquinoxRawData getUsageMonitoringStartRequest(OCFUsageMonitoringRequest OCFUsageMonitoringRequest, String invokeId) {
        String reqData = new Gson().toJson(OCFUsageMonitoringRequest);
        EquinoxRawData rawData = AFDataFactory.createEquinoxRawData();
        this.requestObj = reqData;
        Map<String, String> map = new HashMap<>();
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


    public EquinoxRawData getUsageMonitoringResponse(UsageMonitoringResponse usageMonitoringResponse, String invokeId, String timeout) {
        AFLog.d("Build OCFUsageMonitoringResponse Response ");
        EquinoxRawData rawData = AFDataFactory.createEquinoxRawData();
        String data = new Gson().toJson(usageMonitoringResponse);
        this.requestObj = data;
        Map<String, String> map = new HashMap<>();
        map.put("name", "HTTP");
        map.put("type", "response");
        map.put("ctype", "text/plain");
        map.put("to", PCEFUtils.getWarmConfig(abstractAF, EConfig.RESOURCE_NAME_TEST));
        map.put("oid", "0.0.17.1218.8.7.0");
        map.put("timeout", timeout);

        map.put("invoke", invokeId);

        rawData.setRawDataAttributes(map);
        rawData.setRawDataMessage(data);
        return rawData;
    }


    public Object getRequestObj() {
        return requestObj;
    }
}
