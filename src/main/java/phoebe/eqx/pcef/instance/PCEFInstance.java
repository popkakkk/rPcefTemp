package phoebe.eqx.pcef.instance;

import phoebe.eqx.pcef.core.data.ResourceResponse;
import phoebe.eqx.pcef.core.data.UsageMonitoring;
import phoebe.eqx.pcef.core.model.Quota;
import phoebe.eqx.pcef.core.model.Transaction;
import phoebe.eqx.pcef.message.parser.req.UsageMonitoringRequest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class PCEFInstance {

    private String sessionId; // from SACF

    private TestResponseData testResponseData;
    private Date appointmentDate;
    private Transaction transaction;
    private List<Transaction> otherStartTransactions = new ArrayList<>();


    private UsageMonitoringRequest usageMonitoringRequest;
    private UsageMonitoring usageMonitoring;

    private transient String ocfSessionId; // to OCF


    private boolean quotaExhaust;
    private Quota quotaCommit;
    private transient Map<String, String> countUnitMap;


    public UsageMonitoring getUsageMonitoring() {
        return usageMonitoring;
    }

    public void setUsageMonitoring(UsageMonitoring usageMonitoring) {
        this.usageMonitoring = usageMonitoring;
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


    public List<Transaction> getOtherStartTransactions() {
        return otherStartTransactions;
    }

    public void setOtherStartTransactions(List<Transaction> otherStartTransactions) {
        this.otherStartTransactions = otherStartTransactions;
    }

    public Date getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(Date appointmentDate) {
        this.appointmentDate = appointmentDate;
    }


    public String getOcfSessionId() {
        return ocfSessionId;
    }

    public void setOcfSessionId(String ocfSessionId) {
        this.ocfSessionId = ocfSessionId;
    }

    public boolean isQuotaExhaust() {
        return quotaExhaust;
    }

    public void setQuotaExhaust(boolean quotaExhaust) {
        this.quotaExhaust = quotaExhaust;
    }


    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public Quota getQuotaCommit() {
        return quotaCommit;
    }

    public void setQuotaCommit(Quota quotaCommit) {
        this.quotaCommit = quotaCommit;
    }

    public Map<String, String> getCountUnitMap() {
        return countUnitMap;
    }

    public void setCountUnitMap(Map<String, String> countUnitMap) {
        this.countUnitMap = countUnitMap;
    }
}
