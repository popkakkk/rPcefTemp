package phoebe.eqx.pcef.message.builder;

import com.google.gson.Gson;
import ec02.af.abstracts.AbstractAF;
import ec02.af.data.AFDataFactory;
import ec02.af.utils.AFLog;
import ec02.data.interfaces.EquinoxRawData;
import phoebe.eqx.pcef.enums.config.EConfig;
import phoebe.eqx.pcef.instance.Config;
import phoebe.eqx.pcef.message.builder.req.OCFUsageMonitoringRequest;
import phoebe.eqx.pcef.message.builder.req.RefundTransactionRequest;
import phoebe.eqx.pcef.message.builder.res.GyRARResponse;
import phoebe.eqx.pcef.message.builder.res.RefundManagementResponse;
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


    public EquinoxRawData getResourceIdRequest(String data, String invokeId, String url) {
        String reqData = data;
        EquinoxRawData rawData = AFDataFactory.createEquinoxRawData();
        this.requestObj = reqData;
        Map<String, String> map = new HashMap<>();
        map.put("url", url);
        map.put("name", "HTTP");
        map.put("method", "GET");
        map.put("type", "request");
        map.put("ctype", "application/json");
        map.put("to", Config.RESOURCE_NAME_PRODUCT);
        map.put("timeout", Config.TIMEOUT_PRODUCT);
        map.put("invoke", invokeId);

        rawData.setRawDataAttributes(map);
        rawData.setRawDataMessage(reqData);
        return rawData;
    }


    public EquinoxRawData getOCFUsageMonitoringRequest(OCFUsageMonitoringRequest OCFUsageMonitoringRequest, String invokeId) {
        String reqData = new Gson().toJson(OCFUsageMonitoringRequest);
        EquinoxRawData rawData = AFDataFactory.createEquinoxRawData();
        this.requestObj = reqData;
        Map<String, String> map = new HashMap<>();
        map.put("url", Config.URL_OCF_USAGE_MONITORING);
        map.put("name", "HTTP");
        map.put("method", "POST");
        map.put("type", "request");
        map.put("ctype", "text/plain");
        map.put("to", Config.RESOURCE_NAME_OCF);
        map.put("timeout", Config.TIMEOUT_OCF);
        map.put("invoke", invokeId);

        rawData.setRawDataAttributes(map);
        rawData.setRawDataMessage(reqData);
        return rawData;
    }


    public EquinoxRawData getUsageMonitoringResponse(UsageMonitoringResponse usageMonitoringResponse, String invokeId, String timeout) {

        EquinoxRawData rawData = AFDataFactory.createEquinoxRawData();
        String data = new Gson().toJson(usageMonitoringResponse);
        this.requestObj = data;
        Map<String, String> map = new HashMap<>();
        map.put("name", "HTTP");
        map.put("method", "POST");
        map.put("type", "response");
        map.put("ctype", "text/plain");
        map.put("to", Config.RESOURCE_NAME_SACF);
        map.put("timeout", timeout);
        map.put("invoke", invokeId);

        rawData.setRawDataAttributes(map);
        rawData.setRawDataMessage(data);
        return rawData;
    }

    public EquinoxRawData getGyRARResponse(GyRARResponse gyRARResponse, String invokeId) {
        EquinoxRawData rawData = AFDataFactory.createEquinoxRawData();
        String data = new Gson().toJson(gyRARResponse);
        this.requestObj = data;
        Map<String, String> map = new HashMap<>();
        map.put("name", "HTTP");
        map.put("method", "POST");
        map.put("type", "response");
        map.put("ctype", "text/plain");
        map.put("to", Config.RESOURCE_NAME_OCF);
//        map.put("timeout", timeout);
        map.put("invoke", invokeId);
        rawData.setRawDataAttributes(map);
        rawData.setRawDataMessage(data);
        return rawData;
    }

    public EquinoxRawData getRefundManagementResponse(RefundManagementResponse refundManagementResponse, String invokeId, String timeout) {
        EquinoxRawData rawData = AFDataFactory.createEquinoxRawData();
        String data = new Gson().toJson(refundManagementResponse);
        this.requestObj = data;
        Map<String, String> map = new HashMap<>();
        map.put("name", "HTTP");
        map.put("method", "POST");
        map.put("type", "response");
        map.put("ctype", "text/plain");
        map.put("to", Config.RESOURCE_NAME_OCF);
        map.put("timeout", timeout);
        map.put("invoke", invokeId);
        rawData.setRawDataAttributes(map);
        rawData.setRawDataMessage(data);
        return rawData;
    }

    public EquinoxRawData getRefundTransactionRequest(RefundTransactionRequest refundManagementResponse, String invokeId) {
        EquinoxRawData rawData = AFDataFactory.createEquinoxRawData();
        String data = new Gson().toJson(refundManagementResponse);
        this.requestObj = data;
        Map<String, String> map = new HashMap<>();
        map.put("name", "HTTP");
        map.put("method", "POST");
        map.put("type", "request");
        map.put("ctype", "text/plain");
        map.put("to", Config.RESOURCE_NAME_OCF);
        map.put("timeout", Config.TIMEOUT_OCF);
        map.put("invoke", invokeId);
        rawData.setRawDataAttributes(map);
        rawData.setRawDataMessage(data);
        return rawData;
    }


    public EquinoxRawData recurringVTTimeout(String timeout) {

        EquinoxRawData rawData = AFDataFactory.createEquinoxRawData();
        Map<String, String> map = new HashMap<>();
        map.put("timeout", timeout);
        rawData.setRawDataAttributes(map);
        return rawData;
    }


    public Object getRequestObj() {
        return requestObj;
    }
}
