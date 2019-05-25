package phoebe.eqx.pcef.message.builder.req;

public class RefundTransactionRequest {

    private String command;
    private String actualTime;
    private String userType;
    private String userValue;
    private String resourceId;
    private String resouceName;
    private String tid;
    private String rtid;
    private String monitoringKey;
    private String counterId;
    private String unitType;
    private String usedUnit;
    private String reportingReason;


    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getActualTime() {
        return actualTime;
    }

    public void setActualTime(String actualTime) {
        this.actualTime = actualTime;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getUserValue() {
        return userValue;
    }

    public void setUserValue(String userValue) {
        this.userValue = userValue;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getResouceName() {
        return resouceName;
    }

    public void setResouceName(String resouceName) {
        this.resouceName = resouceName;
    }

    public String getTid() {
        return tid;
    }

    public void setTid(String tid) {
        this.tid = tid;
    }

    public String getRtid() {
        return rtid;
    }

    public void setRtid(String rtid) {
        this.rtid = rtid;
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
