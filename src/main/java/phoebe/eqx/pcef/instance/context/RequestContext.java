package phoebe.eqx.pcef.instance.context;

import ec02.af.utils.AFLog;
import phoebe.eqx.pcef.enums.ERequestType;
import phoebe.eqx.pcef.enums.state.EState;
import phoebe.eqx.pcef.instance.InvokeManager;
import phoebe.eqx.pcef.instance.PCEFInstance;

import java.util.ArrayList;
import java.util.Date;

public class RequestContext {

    //----- instance data -------------------
    private PCEFInstance pcefInstance;
    private InvokeManager invokeManager;

    //----- instance state------------------
    private String requestInvokeId;
    private ERequestType requestType;
    private Date startTime;
    private EState stateL1;
    private EState stateL2;

    private transient boolean hasRequest;
    private transient String reqMessage;


    public RequestContext(String reqMessage, String invoke, ERequestType requestType) {
        this.requestInvokeId = invoke;
        this.reqMessage = reqMessage;
        this.requestType = requestType;
        this.startTime = new Date();
        this.pcefInstance = new PCEFInstance();
        this.invokeManager = new InvokeManager();

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
}