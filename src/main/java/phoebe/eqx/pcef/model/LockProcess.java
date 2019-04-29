package phoebe.eqx.pcef.model;

public class LockProcess {
    private String _id ;
    private String privateId ;
    private int isProcessing ;
    private int sequenceNumber ;
    private String sessionId ;


    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public String getPrivateId() {
        return privateId;
    }

    public void setPrivateId(String privateId) {
        this.privateId = privateId;
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
}
