package phoebe.eqx.pcef.core.data;

import ec02.data.interfaces.EquinoxRawData;
import phoebe.eqx.pcef.enums.EEvent;
import phoebe.eqx.pcef.enums.Operation;
import phoebe.eqx.pcef.utils.PCEFUtils;

import java.util.Date;

public class InvokeObject {
    private String invokeId;


    private Operation operation;
    private EquinoxRawData operationRaw;
    private EquinoxRawData operationRawReq;

    private boolean hasResult;
    private int retry;
    private int retryNumber;
    private Date expireTime;
    private Date reqTime;
    private Date resTime;

    private transient EEvent event;


    public InvokeObject(String invokeId, EquinoxRawData operationRaw, Operation operation, int retry, Date reqTime) {
        this.invokeId = invokeId;

        this.operation = operation;
        this.operationRawReq = operationRaw;
        this.retry = retry;
        this.reqTime = reqTime;
    }


    public void countRetry() {
        this.retry--;
        this.retryNumber++;
    }

    public String getInvokeId() {
        return invokeId;
    }

    public void setInvokeId(String invokeId) {
        this.invokeId = invokeId;
    }

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    public EquinoxRawData getOperationRaw() {
        return operationRaw;
    }

    public void setOperationRaw(EquinoxRawData operationRaw) {
        this.hasResult = true;
        this.operationRaw = operationRaw;
        this.resTime = PCEFUtils.getDate(0);
        this.operationRaw = operationRaw;
    }

    public EquinoxRawData getOperationRawReq() {
        return operationRawReq;
    }

    public void setOperationRawReq(EquinoxRawData operationRawReq) {
        this.operationRawReq = operationRawReq;
    }

    public boolean isHasResult() {
        return hasResult;
    }

    public void setHasResult(boolean hasResult) {
        this.hasResult = hasResult;
    }

    public int getRetry() {
        return retry;
    }

    public void setRetry(int retry) {
        this.retry = retry;
    }

    public Date getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Date expireTime) {
        this.expireTime = expireTime;
    }

    public Date getReqTime() {
        return reqTime;
    }

    public void setReqTime(Date reqTime) {
        this.reqTime = reqTime;
    }

    public Date getResTime() {
        return resTime;
    }

    public void setResTime(Date resTime) {
        this.resTime = resTime;
    }

    public int getRetryNumber() {
        return retryNumber;
    }

    public void setRetryNumber(int retryNumber) {
        this.retryNumber = retryNumber;
    }

    public EEvent getEvent() {
        return event;
    }

    public void setEvent(EEvent event) {
        this.event = event;
    }
}
