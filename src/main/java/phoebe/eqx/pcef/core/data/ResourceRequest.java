package phoebe.eqx.pcef.core.data;

public class ResourceRequest {
    private String resourceId;
    private String resourceName;
    private String rtid;
    private String monitoringKey;
    private String unitType;
    private String usedUnit;
    private String reportingReason;


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

    public String getMonitoringKey() {
        return monitoringKey;
    }

    public void setMonitoringKey(String monitoringKey) {
        this.monitoringKey = monitoringKey;
    }

    public String getUnitType() {
        return unitType;
    }

    public void setUnitType(String unitType) {
        this.unitType = unitType;
    }

    public String getUsedUnit() {
        return usedUnit;
    }

    public void setUsedUnit(String usedUnit) {
        this.usedUnit = usedUnit;
    }

    public String getReportingReason() {
        return reportingReason;
    }

    public void setReportingReason(String reportingReason) {
        this.reportingReason = reportingReason;
    }
}
