package phoebe.eqx.pcef.core.logs.summary;

public class SummaryLogDetail {

    private String invokeId;
    private String reqTime;
    private String resTime;
    private String usedTime;

    //-- External
    private Object req;
    private Object res;

    //Details External
    public SummaryLogDetail(String invokeId, String reqTime, Object req) {
        this.invokeId = invokeId;
        this.reqTime = reqTime;
        this.req = req;
        this.res = "{}";
    }


    public String getInvokeId() {
        return invokeId;
    }

    public void setInvokeId(String invokeId) {
        this.invokeId = invokeId;
    }

    public String getReqTime() {
        return reqTime;
    }

    public void setReqTime(String reqTime) {
        this.reqTime = reqTime;
    }

    public String getResTime() {
        return resTime;
    }

    public void setResTime(String resTime) {
        this.resTime = resTime;
    }

    public String getUsedTime() {
        return usedTime;
    }

    public void setUsedTime(String usedTime) {
        this.usedTime = usedTime;
    }

    public Object getReq() {
        return req;
    }

    public void setReq(Object req) {
        this.req = req;
    }

    public Object getRes() {
        return res;
    }

    public void setRes(Object res) {
        this.res = res;
    }


}
