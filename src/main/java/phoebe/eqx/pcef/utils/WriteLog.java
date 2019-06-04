package phoebe.eqx.pcef.utils;

import ec02.af.abstracts.AbstractAF;
import ec02.af.utils.AFLog;
import phoebe.eqx.pcef.core.exceptions.PCEFException;
import phoebe.eqx.pcef.core.logs.ErrorLog;
import phoebe.eqx.pcef.instance.Config;
import phoebe.eqx.pcef.message.parser.req.GyRARRequest;
import phoebe.eqx.pcef.message.parser.req.RefundManagementRequest;
import phoebe.eqx.pcef.message.parser.req.UsageMonitoringRequest;

public class WriteLog {


    private static void writeLogError(AbstractAF abstractAF, String errorStr) {
        AFLog.d("Write Error Log ..");
        AFLog.w(errorStr);
        abstractAF.getEquinoxUtils().writeLog(Config.LOG_ERROR_NAME, errorStr);
    }

    public static void writeCDRRefund(AbstractAF abstractAF, String cdr) {
        AFLog.d("Write CDR Refund Log ..");
        abstractAF.getEquinoxUtils().writeLog(Config.CDR_REFUND_NAME, cdr);
    }

    public static void writeCDRCharging(AbstractAF abstractAF, String cdr) {
        AFLog.d("Write CDR Charging Log ..");
        abstractAF.getEquinoxUtils().writeLog(Config.CDR_CHARGING_NAME, cdr);
    }

    public static void writeErrorLogUsageMonitoring(AbstractAF abstractAF, PCEFException e, UsageMonitoringRequest usageMonitoringRequest, String resourceId) {

        try {
            //build
            ErrorLog errorLog = new ErrorLog(e.getError().getCode(), e.getError().getDesc(), e.getErrorMsg());
            errorLog.setCommand("usageMonitoring");
            if (usageMonitoringRequest != null) {
                errorLog.setSessionId(usageMonitoringRequest.getSessionId());
                errorLog.setTid(usageMonitoringRequest.getTid());
                errorLog.setRtid(usageMonitoringRequest.getRtid());
                errorLog.setResourceId(resourceId);
                errorLog.setResourceName(usageMonitoringRequest.getResourceName());
                errorLog.setUserType(usageMonitoringRequest.getUserType());
                errorLog.setUserValue(usageMonitoringRequest.getUserValue());
            }

            String errorStr = PCEFUtils.gsonToJson(errorLog);

            writeLogError(abstractAF, errorStr);

        } catch (Exception ex) {
            AFLog.d("Write error log Usage Monitoring error-" + ex.getStackTrace()[0].toString());
        }

    }


    public static void writeErrorLogRefundManagement(AbstractAF abstractAF, PCEFException e, RefundManagementRequest refundManagementRequest) {

        try {

            //build
            ErrorLog errorLog = new ErrorLog(e.getError().getCode(), e.getError().getDesc(), e.getErrorMsg());
            errorLog.setCommand("refundManagement");

            if (refundManagementRequest != null) {
                errorLog.setSessionId(refundManagementRequest.getSessionId());
                errorLog.setTid(refundManagementRequest.getTid());
                errorLog.setRtid(refundManagementRequest.getRtid());
                errorLog.setRefid(refundManagementRequest.getRefId());
            }

            String errorStr = PCEFUtils.gsonToJson(errorLog);

            writeLogError(abstractAF, errorStr);

        } catch (Exception ex) {
            AFLog.d("Write error log Refund Management error-" + ex.getStackTrace()[0].toString());
        }


    }

    public static void writeErrorLogGyRAR(AbstractAF abstractAF, PCEFException e, GyRARRequest gyRARRequest) {

        try {
            AFLog.d("Write Error Log ..");

            //build
            ErrorLog errorLog = new ErrorLog(e.getError().getCode(), e.getError().getDesc(), e.getErrorMsg());
            errorLog.setCommand("GyRAR");

            if (gyRARRequest != null) {
                errorLog.setSessionId(gyRARRequest.getSessionId());
                errorLog.setUserType(gyRARRequest.getUserType());
                errorLog.setUserValue(gyRARRequest.getUserValue());
            }
            String errorStr = PCEFUtils.gsonToJson(errorLog);

            writeLogError(abstractAF, errorStr);

        } catch (Exception ex) {
            AFLog.d("Write error log GyRAR error-" + ex.getStackTrace()[0].toString());
        }


    }

    public static void writeErrorE11Timeout(AbstractAF abstractAF, PCEFException e, String sessionId, String userType, String userValue) {

        try {
            //build
            ErrorLog errorLog = new ErrorLog(e.getError().getCode(), e.getError().getDesc(), e.getErrorMsg());
            errorLog.setCommand("E11 Timeout");
            errorLog.setSessionId(sessionId);
            errorLog.setUserType(userType);
            errorLog.setUserValue(userValue);
            String errorStr = PCEFUtils.gsonToJson(errorLog);

            writeLogError(abstractAF, errorStr);
        } catch (Exception ex) {
            AFLog.d("Write error log E11 Timeout error-" + ex.getStackTrace()[0].toString());
        }
    }


}
