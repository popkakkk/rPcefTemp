package phoebe.eqx.pcef.model;

import java.util.ArrayList;

public class Quota {

    private String _id;
    private String userType;
    private String userValue;
    private String mainProcessing;
    private String appointmentDate;
    private String monitoringKey;
    private String counterId;
    private String quota;
    private String vt;
    private String transactionPerTime;
    private ArrayList<String> resource = new ArrayList<>();


    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
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

    public String getMainProcessing() {
        return mainProcessing;
    }

    public void setMainProcessing(String mainProcessing) {
        this.mainProcessing = mainProcessing;
    }

    public String getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(String appointmentDate) {
        this.appointmentDate = appointmentDate;
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

    public String getQuota() {
        return quota;
    }

    public void setQuota(String quota) {
        this.quota = quota;
    }

    public String getVt() {
        return vt;
    }

    public void setVt(String vt) {
        this.vt = vt;
    }

    public String getTransactionPerTime() {
        return transactionPerTime;
    }

    public void setTransactionPerTime(String transactionPerTime) {
        this.transactionPerTime = transactionPerTime;
    }

    public ArrayList<String> getResource() {
        return resource;
    }

    public void setResource(ArrayList<String> resource) {
        this.resource = resource;
    }
}
