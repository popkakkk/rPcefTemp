package phoebe.eqx.pcef.message;

public class Metering {
    private String sessionId;
    private String tid;
    private String rtid;
    private String actualTime;
    private String app;
    private String clientId;
    private String privateId;
    private String resourceName;


    public Metering() {
    }

    public Metering(String sessionId, String tid, String rtid, String actualTime, String app, String clientId, String privateId, String resourceName) {
        this.sessionId = sessionId;
        this.tid = tid;
        this.rtid = rtid;
        this.actualTime = actualTime;
        this.app = app;
        this.clientId = clientId;
        this.privateId = privateId;
        this.resourceName = resourceName;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
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

    public String getActualTime() {
        return actualTime;
    }

    public void setActualTime(String actualTime) {
        this.actualTime = actualTime;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getPrivateId() {
        return privateId;
    }

    public void setPrivateId(String privateId) {
        this.privateId = privateId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }
}
