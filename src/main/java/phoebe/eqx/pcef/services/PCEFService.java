package phoebe.eqx.pcef.services;

import com.mongodb.DBCursor;
import ec02.af.abstracts.AbstractAF;
import ec02.af.utils.AFLog;
import ec02.data.interfaces.EquinoxRawData;
import phoebe.eqx.pcef.core.PCEFParser;
import phoebe.eqx.pcef.core.data.InvokeObject;
import phoebe.eqx.pcef.core.exceptions.ExtractErrorException;
import phoebe.eqx.pcef.core.exceptions.PCEFException;
import phoebe.eqx.pcef.core.exceptions.ResponseErrorException;
import phoebe.eqx.pcef.core.exceptions.TimeoutException;
import phoebe.eqx.pcef.core.logs.summary.SummaryLog;
import phoebe.eqx.pcef.enums.EEvent;
import phoebe.eqx.pcef.enums.ERequestType;
import phoebe.eqx.pcef.enums.Operation;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.instance.Config;
import phoebe.eqx.pcef.instance.context.RequestContext;
import phoebe.eqx.pcef.message.parser.req.GyRARRequest;
import phoebe.eqx.pcef.services.mogodb.MongoDBConnect;
import phoebe.eqx.pcef.utils.PCEFUtils;

import java.util.Date;

abstract public class PCEFService {
    protected AbstractAF abstractAF;
    protected AppInstance appInstance;
    protected RequestContext context;

    public PCEFService(AppInstance appInstance) {
        this.appInstance = appInstance;
        this.abstractAF = appInstance.getAbstractAF();
        this.context = appInstance.getMyContext();

    }

    protected Object extractResponse(Operation operation) throws TimeoutException, ResponseErrorException, ExtractErrorException {
        InvokeObject invokeObject = context.getInvokeManager().find(operation);

        EEvent event = invokeObject.getEvent();
        if (!EEvent.SUCCESS.equals(event)) {
            if (EEvent.EquinoxMessageTimeout.equals(event)) {
                throw new TimeoutException();
            } else if (EEvent.EquinoxMessageResponseError.equals(event)
                    || EEvent.EquinoxMessageResponseReject.equals(event)
                    || EEvent.EquinoxMessageResponseAbort.equals(event)) {
                throw new ResponseErrorException();
            }
        }

        EquinoxRawData equinoxRawData = invokeObject.getOperationRaw();
        Object result = null;
        PCEFParser parser = null;
        if (Operation.TestOperation.equals(operation)) {
            // parser class
            result = parser.translateTestResponseData();
        } else if (Operation.RefundTransaction.equals(operation)) {
            parser = new PCEFParser(equinoxRawData.getRawDataAttribute("val"));
            result = parser.translateRefundTransactionResponse();
        } else if (Operation.UsageMonitoringStart.equals(operation) || Operation.UsageMonitoringUpdate.equals(operation)) {
            parser = new PCEFParser(equinoxRawData.getRawDataAttribute("val"));
            result = parser.translateUsageMonitoringResponse();
        } else if (Operation.GetResourceId.equals(operation)) {
            parser = new PCEFParser(equinoxRawData.getRawDataMessage());
            result = parser.translateGetResourceId();
        }
        return result;
    }


    protected void invokeExternal(EquinoxRawData rawData, Operation operation, Object reqLogObj) {

        context.setHasRequest(true);
        Date reqTime = PCEFUtils.getDate(0);
        //add Summary Log
        reqLogObj = SummaryLog.getSummaryLogRequest(operation, reqLogObj);
        SummaryLog summaryLog = new SummaryLog(operation.name(), rawData.getInvoke(), reqTime, reqLogObj);
        appInstance.getSummaryLogs().add(summaryLog);

        //add invoke object to list
        context.getInvokeManager().addToInvokeList(rawData.getInvoke(), rawData, operation, reqTime);

    }


    public String getTimeoutFromAppointmentDate() {
        AFLog.d("<---- Calculate Timeout Part ---->");

        if (appInstance.isFinish()) {
            AFLog.d("Application finish not cal timeout from appointmentDate");
            return "10";
        } else {
            Date now = new Date();
            Date appointmentDate = context.getPcefInstance().getProfile().getAppointmentDate();
            long timeout = (Math.abs(appointmentDate.getTime() - now.getTime()) / 1000) + Config.DELAY_TIMEOUT;
            AFLog.d("Current Date    :" + PCEFUtils.isoDateFormatter.format(now));
            AFLog.d("Appointment Date:" + PCEFUtils.isoDateFormatter.format(appointmentDate));
            AFLog.d("delay           :" + Config.DELAY_TIMEOUT);
            AFLog.d("timeout=" + timeout + " second");
            return String.valueOf(timeout);
        }
    }


    public String generateInvokeId(Operation operation) {

        String invokeId = "";
        if (Operation.UsageMonitoringStart.equals(operation)) {
            invokeId = "umStart";
        } else if (Operation.UsageMonitoringUpdate.equals(operation)) {
            invokeId = "umUpdate";
        } else if (Operation.UsageMonitoringStop.equals(operation)) {
            invokeId = "umStop";
        } else if (Operation.RefundTransaction.equals(operation)) {
            invokeId = "refundTransaction";
        } else if (Operation.GetResourceId.equals(operation)) {
            invokeId = "getResourceId";
        }


        return invokeId + "_" /*+ PCEFUtils.getDate(0).getTime()*/;
    }


    public static void buildErrorResponse(AppInstance appInstance) {
        ERequestType requestType = appInstance.getMyContext().getRequestType();

        try {
            //findProfile
            boolean foundProfile = false;

            if (appInstance.getMyContext().getPcefInstance().getProfile() != null) {
                foundProfile = true;
            } else {
                //find profile set profile

                MongoDBConnect dbConnect = null;
                try {
                    dbConnect = new MongoDBConnect(appInstance);
                    String privateId = null;

                    //getPrivateId
                    if (!requestType.equals(ERequestType.REFUND_MANAGEMENT)) {
                        privateId = appInstance.getMyContext().getEqxPropSession();
                    }

                    if (privateId != null) {
                        DBCursor dbCursor = dbConnect.getProfileService().findProfileByPrivateId(privateId);
                        if (dbCursor.hasNext()) {
                            foundProfile = true;
                        }
                    }
                } catch (Exception e) {
                    AFLog.d("find profile for cal E11 timout error-" + e.getStackTrace()[0]);

                } finally {
                    if (dbConnect != null) {
                        dbConnect.closeConnection();
                    }
                }
            }

            //profile not found
            if (!foundProfile) {
                appInstance.setFinish(true);
            }

            //build Response Error
            if (requestType.equals(ERequestType.USAGE_MONITORING)) {
                UsageMonitoringService usageMonitoringService = new UsageMonitoringService(appInstance);
                usageMonitoringService.buildResponseUsageMonitoring(false);
            } else if (requestType.equals(ERequestType.E11_TIMEOUT)) {
                VTTimoutService vtTimoutService = new VTTimoutService(appInstance);
                vtTimoutService.buildRecurringTimout();
            } else if (requestType.equals(ERequestType.GyRAR)) {
                GyRARService gyRARRequest = new GyRARService(appInstance);
                gyRARRequest.buildResponseGyRAR(false);
            } else if (requestType.equals(ERequestType.REFUND_MANAGEMENT)) {
                RefundManagementService refundTransactionService = new RefundManagementService(appInstance);
                refundTransactionService.buildResponseRefundManagement(false);
            }

        } catch (Exception e) {
            AFLog.d("build Response Error fail requestType:" + requestType);
            appInstance.setFinish(true);
        }


    }


}



