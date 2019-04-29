package phoebe.eqx.pcef.core.context;

import phoebe.eqx.pcef.enums.ERequestType;
import phoebe.eqx.pcef.enums.EState;

import java.util.Date;

public class RequestContext {

    private ERequestType requestType;
    private Date startTime;
    private EState stateL1;
    private EState stateL2;

    private transient boolean hasRequest;
    private transient String reqMessage;


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

    public String getReqMessage() {
        return reqMessage;
    }

    public void setReqMessage(String reqMessage) {
        this.reqMessage = reqMessage;
    }
    public boolean isHasRequest() {
        return hasRequest;
    }

    public void setHasRequest(boolean hasRequest) {
        this.hasRequest = hasRequest;
    }
}
