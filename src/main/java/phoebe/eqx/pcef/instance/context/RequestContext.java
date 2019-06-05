package phoebe.eqx.pcef.instance.context;

import phoebe.eqx.pcef.core.exceptions.PCEFException;
import phoebe.eqx.pcef.enums.ERequestType;
import phoebe.eqx.pcef.enums.state.EState;
import phoebe.eqx.pcef.instance.InvokeManager;
import phoebe.eqx.pcef.instance.PCEFInstance;

import java.util.Date;

public class RequestContext {

    //----- instance data -------------------
    private PCEFInstance pcefInstance;
    private InvokeManager invokeManager;

    //----- instance state------------------
    private String requestInvokeId;
    private String eqxPropSession;
    private ERequestType requestType;

    private Date timeoutDate;

    private boolean interval;
    private int intervalRetry;

    private Date startTime;
    private EState stateL1;
    private EState stateL2;
    private EState stateL3;

    private transient boolean hasRequest;
    private transient String reqMessage;

    private transient PCEFException pcefException;

    //flag
    private boolean waitForProcess;

    private boolean lockProfile;
//    private boolean lockQuota;


    public RequestContext(String reqMessage, String invoke, String eqxPropSession, ERequestType requestType) {
        this.requestInvokeId = invoke;
        this.reqMessage = reqMessage;
        this.requestType = requestType;
        this.startTime = new Date();
        this.pcefInstance = new PCEFInstance();
        this.invokeManager = new InvokeManager();
        this.eqxPropSession = eqxPropSession;

    }


    public PCEFInstance getPcefInstance() {
        return pcefInstance;
    }

    public void setPcefInstance(PCEFInstance pcefInstance) {
        this.pcefInstance = pcefInstance;
    }

    public InvokeManager getInvokeManager() {
        return invokeManager;
    }

    public void setInvokeManager(InvokeManager invokeManager) {
        this.invokeManager = invokeManager;
    }

    public String getRequestInvokeId() {
        return requestInvokeId;
    }

    public void setRequestInvokeId(String requestInvokeId) {
        this.requestInvokeId = requestInvokeId;
    }

    public ERequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(ERequestType requestType) {
        this.requestType = requestType;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public EState getStateL1() {
        return stateL1;
    }

    public void setStateL1(EState stateL1) {
        this.stateL1 = stateL1;
    }

    public EState getStateL2() {
        return stateL2;
    }

    public void setStateL2(EState stateL2) {
        this.stateL2 = stateL2;
    }

    public EState getStateL3() {
        return stateL3;
    }

    public void setStateL3(EState stateL3) {
        this.stateL3 = stateL3;
    }

    public boolean isHasRequest() {
        return hasRequest;
    }

    public void setHasRequest(boolean hasRequest) {
        this.hasRequest = hasRequest;
    }

    public String getReqMessage() {
        return reqMessage;
    }

    public void setReqMessage(String reqMessage) {
        this.reqMessage = reqMessage;
    }

    public PCEFException getPcefException() {
        return pcefException;
    }

    public void setPcefException(PCEFException pcefException) {
        this.pcefException = pcefException;
    }

    public String getEqxPropSession() {
        return eqxPropSession;
    }

    public void setEqxPropSession(String eqxPropSession) {
        this.eqxPropSession = eqxPropSession;
    }

    public Date getTimeoutDate() {
        return timeoutDate;
    }

    public void setTimeoutDate(Date timeoutDate) {
        this.timeoutDate = timeoutDate;
    }

    public boolean isInterval() {
        return interval;
    }

    public void setInterval(boolean interval) {
        this.interval = interval;
    }

    public int getIntervalRetry() {
        return intervalRetry;
    }

    public void setIntervalRetry(int intervalRetry) {
        this.intervalRetry = intervalRetry;
    }

    public boolean isWaitForProcess() {
        return waitForProcess;
    }

    public void setWaitForProcess(boolean waitForProcess) {
        this.waitForProcess = waitForProcess;
    }

    public boolean isLockProfile() {
        return lockProfile;
    }

    public void setLockProfile(boolean lockProfile) {
        this.lockProfile = lockProfile;
    }
}
