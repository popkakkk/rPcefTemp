package phoebe.eqx.pcef.core;

import com.google.gson.Gson;
import phoebe.eqx.pcef.message.parser.res.OCFUsageMonitoringResponse;
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

    public OCFUsageMonitoringResponse translateUsageMonitoringResponse() {
        return gson.fromJson(message, OCFUsageMonitoringResponse.class);
    }

    public UsageMonitoringRequest translateUsageMonitoringRequest() {
        return gson.fromJson(message, UsageMonitoringRequest.class);
    }


}
