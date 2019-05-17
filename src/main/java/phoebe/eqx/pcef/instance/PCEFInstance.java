package phoebe.eqx.pcef.instance;

import phoebe.eqx.pcef.core.model.Quota;
import phoebe.eqx.pcef.core.model.Transaction;
import phoebe.eqx.pcef.message.builder.req.OCFUsageMonitoringRequest;
import phoebe.eqx.pcef.message.parser.req.UsageMonitoringRequest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class PCEFInstance {

    private String sessionId; // from SACF

    private TestResponseData testResponseData;

    private Transaction transaction;
    private List<Transaction> otherStartTransactions = new ArrayList<>();


    private UsageMonitoringRequest usageMonitoringRequest;

    private OCFUsageMonitoringRequest ocfUsageMonitoringRequest;

    //Profile
    private Integer sequenceNumber;
    private Date appointmentDate;
    private String ocfSessionId; // to OCF

    //Commit Part
    private boolean quotaExhaust;
    private Quota quotaToCommit;
    private transient Map<String, String> countUnitMap;


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

    public Quota getQuotaToCommit() {
        return quotaToCommit;
    }

    public void setQuotaForCommit(Quota quotaCommit) {
        this.quotaToCommit = quotaCommit;
    }

    public Map<String, String> getCountUnitMap() {
        return countUnitMap;
    }

    public void setCountUnitMap(Map<String, String> countUnitMap) {
        this.countUnitMap = countUnitMap;
    }

    public OCFUsageMonitoringRequest getOcfUsageMonitoringRequest() {
        return ocfUsageMonitoringRequest;
    }

    public void setOcfUsageMonitoringRequest(OCFUsageMonitoringRequest ocfUsageMonitoringRequest) {
        this.ocfUsageMonitoringRequest = ocfUsageMonitoringRequest;
    }

    public void increasSequenceNumber() {
        sequenceNumber++;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(Integer sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public void setQuotaToCommit(Quota quotaToCommit) {
        this.quotaToCommit = quotaToCommit;
    }
}
