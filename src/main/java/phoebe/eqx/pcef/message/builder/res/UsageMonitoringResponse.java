package phoebe.eqx.pcef.message.builder.res;

public class UsageMonitoringResponse {
    private String sessionId;
    private String rtid;
    private String tid;
    private String status;
    private String devMessage;


    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getRtid() {
        return rtid;
    }

    public void setRtid(String rtid) {
        this.rtid = rtid;
    }

    public String getTid() {
        return tid;
    }

    public void setTid(String tid) {
        this.tid = tid;
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
