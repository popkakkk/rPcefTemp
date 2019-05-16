package phoebe.eqx.pcef.core.data;

public class ResourceResponse {
    private String rtid;
    private String resourceId;
    private String resourceName;
    private String resultCode;
    private String resultDesc;
    private String monitoringKey;
    private String counterId;
    private QuotaByKey quotaByKey;
    private RateLimitByKey rateLimitByKey;


    public String getRtid() {
        return rtid;
    }

    public void setRtid(String rtid) {
        this.rtid = rtid;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getResultCode() {
        return resultCode;
    }

    public void setResultCode(String resultCode) {
        this.resultCode = resultCode;
    }

    public String getResultDesc() {
        return resultDesc;
    }

    public void setResultDesc(String resultDesc) {
        this.resultDesc = resultDesc;
    }

    public String getMonitoringKey() {
        return monitoringKey;
    }

    public void setMonitoringKey(String monitoringKey) {
        this.monitoringKey = monitoringKey;
    }

    public String getCounterId() {
        return counterId;
    }

    public void setCounterId(String counterId) {
        this.counterId = counterId;
    }

    public QuotaByKey getQuotaByKey() {
        return quotaByKey;
    }

    public void setQuotaByKey(QuotaByKey quotaByKey) {
        this.quotaByKey = quotaByKey;
    }

    public RateLimitByKey getRateLimitByKey() {
        return rateLimitByKey;
    }

    public void setRateLimitByKey(RateLimitByKey rateLimitByKey) {
        this.rateLimitByKey = rateLimitByKey;
    }
}
