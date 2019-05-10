package phoebe.eqx.pcef.core;

import com.google.gson.Gson;
import phoebe.eqx.pcef.core.data.OCFUsageMonitoring;
import phoebe.eqx.pcef.instance.TestResponseData;
import phoebe.eqx.pcef.message.parser.req.UsageMonitoringRequest;

public class PCEFParser {
    private String message;
    private Gson gson = new Gson();

    public PCEFParser(String message) {
        this.message = message;
    }

    public TestResponseData translateTestResponseData() {
        return new TestResponseData();
    }

    public OCFUsageMonitoring translateUsageMonitoringResponse() {
        return gson.fromJson(message, OCFUsageMonitoring.class);
    }

    public UsageMonitoringRequest translateUsageMonitoringRequest() {
        return gson.fromJson(message, UsageMonitoringRequest.class);
    }


}
