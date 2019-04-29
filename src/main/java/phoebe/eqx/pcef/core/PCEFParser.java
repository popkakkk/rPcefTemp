package phoebe.eqx.pcef.core;

import com.google.gson.Gson;
import phoebe.eqx.pcef.core.data.MyDB;
import phoebe.eqx.pcef.instance.TestResponseData;

public class PCEFParser {
    private String message;
    private Gson gson = new Gson();

    public PCEFParser(String message) {
        this.message = message;
    }

    public TestResponseData translateTestResponseData() {
        return new TestResponseData();
    }

    public MyDB translateUsageMonitoringResponse() {
        return gson.fromJson(message, MyDB.class);
    }


}
