package phoebe.eqx.pcef.core.cdr;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "Ctudr")
@XmlAccessorType(XmlAccessType.FIELD)
public class Ctudr {

    private String app;
    private String actualtime;
    private String resourceid;
    private String clientid;
    private String unittype;
    private String usedunit;
    private String rtid;
    private String usertype;
    private String tid;
    private String counterid;
    private String hostname;
    private String resourcename;
    private String monitoringkey;
    private String uservalue;
    private String cid;
    private String status;


    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getActualtime() {
        return actualtime;
    }

    public void setActualtime(String actualtime) {
        this.actualtime = actualtime;
    }

    public String getResourceid() {
        return resourceid;
    }

    public void setResourceid(String resourceid) {
        this.resourceid = resourceid;
    }

    public String getClientid() {
        return clientid;
    }

    public void setClientid(String clientid) {
        this.clientid = clientid;
    }

    public String getUnittype() {
        return unittype;
    }

    public void setUnittype(String unittype) {
        this.unittype = unittype;
    }

    public String getUsedunit() {
        return usedunit;
    }

    public void setUsedunit(String usedunit) {
        this.usedunit = usedunit;
    }

    public String getRtid() {
        return rtid;
    }

    public void setRtid(String rtid) {
        this.rtid = rtid;
    }

    public String getUsertype() {
        return usertype;
    }

    public void setUsertype(String usertype) {
        this.usertype = usertype;
    }

    public String getTid() {
        return tid;
    }

    public void setTid(String tid) {
        this.tid = tid;
    }

    public String getCounterid() {
        return counterid;
    }

    public void setCounterid(String counterid) {
        this.counterid = counterid;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getResourcename() {
        return resourcename;
    }

    public void setResourcename(String resourcename) {
        this.resourcename = resourcename;
    }

    public String getMonitoringkey() {
        return monitoringkey;
    }

    public void setMonitoringkey(String monitoringkey) {
        this.monitoringkey = monitoringkey;
    }

    public String getUservalue() {
        return uservalue;
    }

    public void setUservalue(String uservalue) {
        this.uservalue = uservalue;
    }

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Ctudr [app = " + app + ", actualtime = " + actualtime + ", resourceid = " + resourceid + ", clientid = " + clientid + ", unittype = " + unittype + ", usedunit = " + usedunit + ", rtid = " + rtid + ", usertype = " + usertype + ", tid = " + tid + ", counterid = " + counterid + ", hostname = " + hostname + ", resourcename = " + resourcename + ", monitoringkey = " + monitoringkey + ", uservalue = " + uservalue + ", cid = " + cid + ", status = " + status + "]";
    }

}
