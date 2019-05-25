package phoebe.eqx.pcef.message.parser.res;

public class RefundTransactionResponse {

    private String command;
    private String userType;
    private String userValue;
    private String tid;
    private String rtid;
    private String status;
    private String devMessage;

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDevMessage() {
        return devMessage;
    }

    public void setDevMessage(String devMessage) {
        this.devMessage = devMessage;
    }
}
