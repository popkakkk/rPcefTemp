package phoebe.eqx.pcef.services;

import ec02.af.abstracts.AbstractAF;
import ec02.data.interfaces.EquinoxRawData;
import phoebe.eqx.pcef.core.PCEFParser;
import phoebe.eqx.pcef.core.data.InvokeObject;
import phoebe.eqx.pcef.core.exceptions.ResponseErrorException;
import phoebe.eqx.pcef.core.exceptions.TimeoutException;
import phoebe.eqx.pcef.core.logs.summary.SummaryLog;
import phoebe.eqx.pcef.enums.EEvent;
import phoebe.eqx.pcef.enums.Operation;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.utils.PCEFUtils;

import java.util.Date;

abstract public class PCEFService {
    protected AbstractAF abstractAF;
    protected AppInstance appInstance;

    public PCEFService(AppInstance appInstance) {
        this.appInstance = appInstance;
        this.abstractAF = appInstance.getAbstractAF();

    }

    protected Object extractResponse(Operation operation) throws Exception {
        InvokeObject invokeObject = this.appInstance.getInvokeManager().find(operation);

        EEvent event = invokeObject.getEvent();
        if (!EEvent.SUCCESS.equals(event)) {
            if (EEvent.EquinoxMessageTimeout.equals(event)) {
                throw new TimeoutException();
            } else if (EEvent.EquinoxMessageResponseError.equals(event)
                    || EEvent.EquinoxMessageResponseReject.equals(event)
                    || EEvent.EquinoxMessageResponseReject.equals(event)) {
                throw new ResponseErrorException();
            }
        }

        EquinoxRawData equinoxRawData = invokeObject.getOperationRaw();
        Object result = null;
        PCEFParser parser = null;
        if (Operation.TestOperation.equals(operation)) {
            // parser class
            result = parser.translateTestResponseData();
        } else if (Operation.QueryDBPrivateID.equals(operation)) {
            parser = new PCEFParser(equinoxRawData.getRawDataMessage());
            result = parser.translateUsageMonitoringResponse();
        } else if (Operation.UsageMonitoringStart.equals(operation) || Operation.UsageMonitoringUpdate.equals(operation)) {
            parser = new PCEFParser(equinoxRawData.getRawDataAttribute("val"));
            result = parser.translateUsageMonitoringResponse();
        } else if (Operation.GetResourceId.equals(operation) ) {
            parser = new PCEFParser(equinoxRawData.getRawDataMessage());
            result = parser.translateGetResourceId();
        }


        return result;

    }


    protected void invokeExternal(EquinoxRawData rawData, Operation operation, Object reqLogObj) throws Exception {
        try {
            appInstance.setHasRequest(true);
            Date reqTime = PCEFUtils.getDate(0);
            //add Summary Log
            reqLogObj = SummaryLog.getSummaryLogRequest(operation, reqLogObj);
            SummaryLog summaryLog = new SummaryLog(operation.name(), rawData.getInvoke(), reqTime, reqLogObj);
            appInstance.getSummaryLogs().add(summaryLog);

            //add invoke object to list
            appInstance.getInvokeManager().addToInvokeList(rawData.getInvoke(), rawData, operation, reqTime);
        } catch (Exception e) {
            throw e;
        }
    }

}
