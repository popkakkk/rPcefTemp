package phoebe.eqx.pcef.core.data;

public class Resource {
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
}
