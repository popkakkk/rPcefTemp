package phoebe.eqx.pcef.instance;

import phoebe.eqx.pcef.message.Metering;

public class PCEFInstance {

    private TestResponseData testResponseData;
    private Metering metering;
    private String resource_id_test = "1234";


    public Metering getMetering() {
        return metering;
    }

    public void setMetering(Metering metering) {
        this.metering = metering;
    }

    public TestResponseData getTestResponseData() {
        return testResponseData;
    }

    public void setTestResponseData(TestResponseData testResponseData) {
        this.testResponseData = testResponseData;
    }

    public String getResource_id_test() {
        return resource_id_test;
    }
}
