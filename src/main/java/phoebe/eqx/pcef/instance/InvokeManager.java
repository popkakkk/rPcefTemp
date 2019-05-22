package phoebe.eqx.pcef.instance;

import ec02.af.utils.AFLog;
import ec02.data.interfaces.EquinoxRawData;
import phoebe.eqx.pcef.core.data.InvokeObject;
import phoebe.eqx.pcef.enums.EEvent;
import phoebe.eqx.pcef.enums.Operation;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.instance.Config;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class InvokeManager {
    private List<InvokeObject> list;
    private transient List<InvokeObject> newList = new ArrayList<>();

    public void putRawData(EquinoxRawData rawData, EEvent event) {
        String invoke = rawData.getInvoke();
        for (InvokeObject invokeObject : list) {
            if (invoke.equals(invokeObject.getInvokeId())) {
                AFLog.d("raw data :" + rawData);
                AFLog.d("set event :" + event);

                invokeObject.setEvent(event);
                invokeObject.setOperationRaw(rawData);
            }
        }
    }


    public void setEventTimeout() {
        for (InvokeObject invokeObject : list) {
            if (!invokeObject.isHasResult()) {
                invokeObject.setEvent(EEvent.EquinoxMessageTimeout);
            }
        }
    }


    public boolean dataResponseComplete() {
        boolean complete = true;
        for (int i = 0, listSize = list.size(); i < listSize; i++) {
            InvokeObject object = list.get(i);
            if (object.isHasResult()) {
                AFLog.d("OK [" + i + "] " + object);
            } else {
                complete = false;
                AFLog.d("NO [" + i + "] " + object);
            }
        }
        return complete;
    }


    public InvokeObject find(Operation clientOperation) {
        for (InvokeObject invokeObject : list) {
            Operation operation = invokeObject.getOperation();
            if (clientOperation.equals(operation)) {
                AFLog.d("InvokeExternal found " + operation + " " + invokeObject);
                return invokeObject;
            }
        }
        return null;
    }


    public void patchResponse(List<EquinoxRawData> outList) {
        list = newList;
        for (InvokeObject invokeObject : list) {
            outList.add(invokeObject.getOperationRawReq());
        }
    }


    public void addToInvokeList(String invokeId, EquinoxRawData rawData, Operation
            operation, Date reqTime) {
        int retryNumber = this.retrieveRetryNumber(operation);
        InvokeObject invokeObject = new InvokeObject(invokeId, rawData, operation, retryNumber, reqTime);
        newList.add(invokeObject);
    }


    private int retrieveRetryNumber(Operation operation) {
        try {
            int retryNumber = 0;
            if (Operation.GetResourceId.equals(operation)) {
                retryNumber = Config.RETRY_PRODUCT_GET_RESOURCE_ID;
            } else if (Operation.UsageMonitoringStart.equals(operation) || Operation.UsageMonitoringUpdate.equals(operation) || Operation.UsageMonitoringStop.equals(operation)) {
                retryNumber = Config.RETRY_OCF_USAGE_MONITORING;
            }

            AFLog.d("Operation[" + operation + "] has configured retryTimeout number as " + retryNumber);
            return retryNumber;
        } catch (Exception ex) {
            AFLog.w("Retrieve Retry number error, please check configuration!!!");
            return 0;
        }
    }


    private List<InvokeObject> findRetryTimeoutList() throws Exception {
        ArrayList<InvokeObject> retryList = new ArrayList<InvokeObject>();
        for (InvokeObject invokeObject : list) {
            if (!invokeObject.isHasResult()) {
                if (invokeObject.getRetry() != 0) {
                    retryList.add(invokeObject);
                }
            }
        }
        return retryList;
    }


    public boolean retryTimeout(AppInstance appInstance) {
        try {
            List<InvokeObject> retryList = findRetryTimeoutList();
            if (retryList.size() == 0) {
                return false;
            }
            ArrayList<EquinoxRawData> rawOutList = appInstance.getOutList();
            for (InvokeObject invokeObject : retryList) {
                invokeObject.countRetry();
                EquinoxRawData rawData = invokeObject.getOperationRawReq();
                Operation operation = invokeObject.getOperation();
                if (operation != null) {
                    String oldInvoke = invokeObject.getInvokeId();
                    int in = oldInvoke.indexOf(":");
                    if (in != -1) {
                        oldInvoke = invokeObject.getInvokeId().substring(0, in);
                    }
                    String newInvokeId = oldInvoke + ":retryTimeout:" + invokeObject.getRetryNumber();

                    invokeObject.setInvokeId(newInvokeId);
                    rawData.setInvoke(newInvokeId);
                    rawOutList.add(rawData);
                }
            }
        } catch (Exception e) {
        }
        return true;
    }

    public List<InvokeObject> getList() {
        return list;
    }

}
