package phoebe.eqx.pcef.services;

import ec02.af.abstracts.AbstractAF;
import ec02.data.interfaces.EquinoxRawData;
import phoebe.eqx.pcef.core.PCEFParser;
import phoebe.eqx.pcef.core.context.InvokeManager;
import phoebe.eqx.pcef.core.context.RequestContext;
import phoebe.eqx.pcef.core.data.InvokeObject;
import phoebe.eqx.pcef.core.exceptions.ResponseErrorException;
import phoebe.eqx.pcef.core.exceptions.TimeoutException;
import phoebe.eqx.pcef.core.logs.summary.SummaryLog;
import phoebe.eqx.pcef.enums.EEvent;
import phoebe.eqx.pcef.enums.Operation;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.message.MessagePool;
import phoebe.eqx.pcef.utils.PCEFUtils;

import java.util.Date;

public class PCEFService {
    protected AbstractAF abstractAF;
    protected AppInstance appInstance;
    protected RequestContext context;
    protected InvokeManager invokeManager;

    protected MessagePool msgPool;



    public PCEFService(AppInstance appInstance) {
        this.abstractAF = appInstance.getAbstractAF();
        this.appInstance = appInstance;
        this.context = appInstance.getRequestContext();
        this.invokeManager = appInstance.getInvokeManager();
        this.msgPool = new MessagePool(abstractAF, appInstance);
    }


    public Object extractResponse(Operation operation) throws Exception {
        InvokeObject invokeObject = this.invokeManager.find(Operation.TestOperation);

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
        PCEFParser parser = new PCEFParser(equinoxRawData.getRawDataMessage());
        if (Operation.TestOperation.equals(operation)) {
            // parser class
            result = parser.translateTestResponseData();
        } else if (Operation.QueryDBPrivateID.equals(operation)) {
            result = parser.translateUsageMonitoringResponse();
        }


        return result;

    }


    public void invokeExternal(EquinoxRawData rawData, Operation operation, Object reqLogObj) throws Exception {
        try {
            appInstance.getRequestContext().setHasRequest(true);
            Date reqTime = PCEFUtils.getDate(0);
            //add Summary Log
            reqLogObj = SummaryLog.getSummaryLogRequest(operation, reqLogObj);
            SummaryLog summaryLog = new SummaryLog(operation.name(), rawData.getInvoke(), reqTime, reqLogObj);
            appInstance.getSummaryLogs().add(summaryLog);

            //add invoke object to list
            invokeManager.addToInvokeList(rawData.getInvoke(), rawData, operation, reqTime);
        } catch (Exception e) {
            throw e;
        }
    }

}
