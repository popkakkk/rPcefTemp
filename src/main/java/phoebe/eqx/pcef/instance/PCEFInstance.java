package phoebe.eqx.pcef.instance;

import phoebe.eqx.pcef.message.req.MeteringRequest;

public class PCEFInstance {

    private TestResponseData testResponseData;
    private MeteringRequest meteringRequest;


    public MeteringRequest getMeteringRequest() {
        return meteringRequest;
    }

    public void setMeteringRequest(MeteringRequest meteringRequest) {
        this.meteringRequest = meteringRequest;
    }

    public TestResponseData getTestResponseData() {
        return testResponseData;
    }

    public void setTestResponseData(TestResponseData testResponseData) {
        this.testResponseData = testResponseData;
    }
}
