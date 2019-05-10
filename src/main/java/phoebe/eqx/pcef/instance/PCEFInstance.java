package phoebe.eqx.pcef.instance;

import phoebe.eqx.pcef.core.data.OCFUsageMonitoring;
import phoebe.eqx.pcef.message.parser.req.UsageMonitoringRequest;
import phoebe.eqx.pcef.model.Transaction;

public class PCEFInstance {

    private String sessionId; // from SACF
    private TestResponseData testResponseData;
    private Transaction transaction;

    private UsageMonitoringRequest usageMonitoringRequest;
    private OCFUsageMonitoring ocfUsageMonitoring;


    public OCFUsageMonitoring getOcfUsageMonitoring() {
        return ocfUsageMonitoring;
    }

    public void setOcfUsageMonitoring(OCFUsageMonitoring ocfUsageMonitoring) {
        this.ocfUsageMonitoring = ocfUsageMonitoring;
    }

    public UsageMonitoringRequest getUsageMonitoringRequest() {
        return usageMonitoringRequest;
    }

    public void setUsageMonitoringRequest(UsageMonitoringRequest usageMonitoringRequest) {
        this.usageMonitoringRequest = usageMonitoringRequest;
    }


    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }


    public TestResponseData getTestResponseData() {
        return testResponseData;
    }

    public void setTestResponseData(TestResponseData testResponseData) {
        this.testResponseData = testResponseData;
    }


    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }
}
