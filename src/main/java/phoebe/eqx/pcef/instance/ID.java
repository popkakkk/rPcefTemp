package phoebe.eqx.pcef.instance;

public class ID {
    private String monitoringKey;
    private String resourceId;
    private String resourceName;

    public String getMonitoringKey() {
        return monitoringKey;
    }

    public void setMonitoringKey(String monitoringKey) {
        this.monitoringKey = monitoringKey;
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