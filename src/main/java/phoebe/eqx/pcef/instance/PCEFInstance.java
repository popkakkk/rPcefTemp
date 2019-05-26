package phoebe.eqx.pcef.instance;

import phoebe.eqx.pcef.core.model.Profile;
import phoebe.eqx.pcef.core.model.Transaction;
import phoebe.eqx.pcef.message.builder.req.OCFUsageMonitoringRequest;
import phoebe.eqx.pcef.message.parser.req.GyRARRequest;
import phoebe.eqx.pcef.message.parser.req.RefundManagementRequest;
import phoebe.eqx.pcef.message.parser.req.UsageMonitoringRequest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PCEFInstance {

    private String sessionId; // from SACF
    private Date startTime = new Date();

    private UsageMonitoringRequest usageMonitoringRequest;
    private GyRARRequest gyRARRequest;
    private RefundManagementRequest refundManagementRequest;


    private Profile profile;
    private Transaction transaction;
    private List<Transaction> otherStartTransactions = new ArrayList<>();


    private OCFUsageMonitoringRequest ocfUsageMonitoringRequest;


    //Commit Part

    List<CommitData> commitDatas = new ArrayList<>();


    public int getQuotaCommitSize() {
        List<String> mkList = new ArrayList<>();

        for (CommitData commitData : commitDatas) {
            if (!mkList.contains(commitData.get_id().getMonitoringKey())) {
                mkList.add(commitData.get_id().getMonitoringKey());
            }
        }
        return mkList.size();
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

    public List<Transaction> getOtherStartTransactions() {
        return otherStartTransactions;
    }

    public void setOtherStartTransactions(List<Transaction> otherStartTransactions) {
        this.otherStartTransactions = otherStartTransactions;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }


    public OCFUsageMonitoringRequest getOcfUsageMonitoringRequest() {
        return ocfUsageMonitoringRequest;
    }

    public void setOcfUsageMonitoringRequest(OCFUsageMonitoringRequest ocfUsageMonitoringRequest) {
        this.ocfUsageMonitoringRequest = ocfUsageMonitoringRequest;
    }


    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    public Date getStartTime() {
        return startTime;
    }


    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public GyRARRequest getGyRARRequest() {
        return gyRARRequest;
    }

    public void setGyRARRequest(GyRARRequest gyRARRequest) {
        this.gyRARRequest = gyRARRequest;
    }

    public RefundManagementRequest getRefundManagementRequest() {
        return refundManagementRequest;
    }

    public void setRefundManagementRequest(RefundManagementRequest refundManagementRequest) {
        this.refundManagementRequest = refundManagementRequest;
    }

    public List<CommitData> getCommitDatas() {
        return commitDatas;
    }

    public void setCommitDatas(List<CommitData> commitDatas) {
        this.commitDatas = commitDatas;
    }
}
