package phoebe.eqx.pcef.model;

import java.util.Date;

public class Profile {
    private String _id;
    private String userType;
    private String userValue;
    private int isProcessing;
    private int sequenceNumber;
    private String sessionId;
    private Date appointmentData;


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

    public int getIsProcessing() {
        return isProcessing;
    }

    public void setIsProcessing(int isProcessing) {
        this.isProcessing = isProcessing;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Date getAppointmentData() {
        return appointmentData;
    }

    public void setAppointmentData(Date appointmentData) {
        this.appointmentData = appointmentData;
    }
}
