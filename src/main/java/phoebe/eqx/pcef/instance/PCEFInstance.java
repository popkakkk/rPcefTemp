package phoebe.eqx.pcef.instance;

import phoebe.eqx.pcef.core.data.OCFUsageMonitoring;
import phoebe.eqx.pcef.message.parser.req.UsageMonitoringRequest;
import phoebe.eqx.pcef.model.Transaction;

import java.util.Date;
import java.util.List;

public class PCEFInstance {

    private String sessionId; // from SACF
    private String tid;// transaction id of instance
    private TestResponseData testResponseData;
    private Date appointmentDate;
    private List<Transaction> transactions; //get[0] is transaction of instance

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


    public String getTid() {
        return tid;
    }

    public void setTid(String tid) {
        this.tid = tid;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    public Date getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(Date appointmentDate) {
        this.appointmentDate = appointmentDate;
    }
}
