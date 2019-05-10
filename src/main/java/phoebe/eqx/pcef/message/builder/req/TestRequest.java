package phoebe.eqx.pcef.message.builder.req;

public class TestRequest {

    String methodVersion;
    String data;

    public TestRequest(String methodVersion, String data) {
        this.methodVersion = methodVersion;
        this.data = data;
    }
}
