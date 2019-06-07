package phoebe.eqx.pcef.utils;

import ec02.af.abstracts.AbstractAF;
import org.apache.commons.lang3.StringUtils;
import phoebe.eqx.pcef.core.exceptions.MissingParameterException;
import phoebe.eqx.pcef.core.exceptions.WrongFormatException;
import phoebe.eqx.pcef.enums.EError;
import phoebe.eqx.pcef.enums.stats.EStatCmd;
import phoebe.eqx.pcef.enums.stats.EStatMode;
import phoebe.eqx.pcef.message.parser.req.GyRARRequest;
import phoebe.eqx.pcef.message.parser.req.RefundManagementRequest;
import phoebe.eqx.pcef.message.parser.req.UsageMonitoringRequest;
import phoebe.eqx.pcef.message.parser.res.OCFUsageMonitoringResponse;
import phoebe.eqx.pcef.message.parser.res.RefundTransactionResponse;
import phoebe.eqx.pcef.message.parser.res.product.GetResourceIdResponse;
import phoebe.eqx.pcef.services.OCFUsageMonitoringService;

public class ValidateMessage {



    public static void validateUsageMonitoringRequest(UsageMonitoringRequest usageMonitoringRequest) throws MissingParameterException, WrongFormatException {

        if (StringUtils.isBlank(usageMonitoringRequest.getCommand()) ||
                StringUtils.isBlank(usageMonitoringRequest.getSessionId()) ||
                StringUtils.isBlank(usageMonitoringRequest.getTid()) ||
                StringUtils.isBlank(usageMonitoringRequest.getRtid()) ||
                StringUtils.isBlank(usageMonitoringRequest.getActualTime()) ||
                StringUtils.isBlank(usageMonitoringRequest.getClientId()) ||
                StringUtils.isBlank(usageMonitoringRequest.getUserType()) ||
                StringUtils.isBlank(usageMonitoringRequest.getUserValue()) ||
                StringUtils.isBlank(usageMonitoringRequest.getResourceName())) {
            throw new MissingParameterException("", EError.USAGE_MONITORING_MISSING_PARAMETER);
        }

        if (!usageMonitoringRequest.getCommand().equals("usageMonitoring")) {
            throw new WrongFormatException("", EError.USAGE_MONITORING_WRONG_FORMAT);
        }
    }

    public static void validateGetResourceIdResponse(GetResourceIdResponse getResourceIdResponse, AbstractAF abstractAF) throws MissingParameterException, WrongFormatException {

        if (StringUtils.isBlank(getResourceIdResponse.getResultData().getProducts().get(0).getProductId())) {
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.MISSING_PARAMETER, EStatCmd.receive_Get_Product_response);
            throw new MissingParameterException("productId is blank", EError.GET_PRODUCT_RESPONSE_MISSING_PARAMETER);
        }

        if (getResourceIdResponse == null) {
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.WRONG_FORMAT, EStatCmd.receive_Get_Product_response);
            throw new WrongFormatException("", EError.GET_PRODUCT_RESPONSE_WRONG_FORMAT);
        }

    }


    private static boolean checkOCFUsageMonitoringMissingParameter(OCFUsageMonitoringResponse ocfUsageMonitoringResponse) {
        if (StringUtils.isBlank(ocfUsageMonitoringResponse.getCommand()) ||
                StringUtils.isBlank(ocfUsageMonitoringResponse.getSessionId()) ||
                StringUtils.isBlank(ocfUsageMonitoringResponse.getTid()) ||
                StringUtils.isBlank(ocfUsageMonitoringResponse.getRequestNumber()) ||
                StringUtils.isBlank(ocfUsageMonitoringResponse.getUserType()) ||
                StringUtils.isBlank(ocfUsageMonitoringResponse.getUserValue()) ||
                StringUtils.isBlank(ocfUsageMonitoringResponse.getStatus()) ||
                StringUtils.isBlank(ocfUsageMonitoringResponse.getDevMessage()) /*||
                ocfUsageMonitoringResponse.getResources() == null*/) {
            return true;
        }
        return false;
    }

    public static void validateUsageMonitoringStart(OCFUsageMonitoringResponse ocfUsageMonitoringResponse, AbstractAF abstractAF) throws MissingParameterException, WrongFormatException {

        if (checkOCFUsageMonitoringMissingParameter(ocfUsageMonitoringResponse)) {
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.MISSING_PARAMETER, EStatCmd.receive_Usage_Monitoring_Start_response);
            throw new MissingParameterException("", EError.USAGE_MONITORING_START_RESPONSE_MISSING_PARAMETER);
        }

        if (!ocfUsageMonitoringResponse.getCommand().equals(OCFUsageMonitoringService.CMD_UM_START)) {
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.WRONG_FORMAT, EStatCmd.receive_Usage_Monitoring_Start_response);
            throw new WrongFormatException("", EError.USAGE_MONITORING_START_RESPONSE_WRONG_FORMAT);
        }

    }

    public static void validateUsageMonitoringUpdate(OCFUsageMonitoringResponse ocfUsageMonitoringResponse, AbstractAF abstractAF) throws MissingParameterException, WrongFormatException {

        if (checkOCFUsageMonitoringMissingParameter(ocfUsageMonitoringResponse)) {
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.WRONG_FORMAT, EStatCmd.receive_Usage_Monitoring_Update_response);
            throw new MissingParameterException("", EError.USAGE_MONITORING_UPDATE_RESPONSE_MISSING_PARAMETER);
        }

        if (!ocfUsageMonitoringResponse.getCommand().equals(OCFUsageMonitoringService.CMD_UM_UPDATE)) {
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.WRONG_FORMAT, EStatCmd.receive_Usage_Monitoring_Update_response);
            throw new WrongFormatException("", EError.USAGE_MONITORING_UPDATE_RESPONSE_WRONG_FORMAT);
        }

    }

    public static void validateUsageMonitoringStop(OCFUsageMonitoringResponse ocfUsageMonitoringResponse, AbstractAF abstractAF) throws MissingParameterException, WrongFormatException {

        if (checkOCFUsageMonitoringMissingParameter(ocfUsageMonitoringResponse)) {
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.MISSING_PARAMETER, EStatCmd.receive_Usage_Monitoring_Stop_response);
            throw new MissingParameterException("", EError.USAGE_MONITORING_STOP_RESPONSE_MISSING_PARAMETER);
        }

        if (!ocfUsageMonitoringResponse.getCommand().equals(OCFUsageMonitoringService.CMD_UM_STOP)) {
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.WRONG_FORMAT, EStatCmd.receive_Usage_Monitoring_Stop_response);
            throw new WrongFormatException("", EError.USAGE_MONITORING_STOP_RESPONSE_WRONG_FORMAT);
        }

    }

    public static void validateGyRAR(GyRARRequest gyRARRequest, AbstractAF abstractAF) throws MissingParameterException, WrongFormatException {

        if (gyRARRequest == null) {
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.MISSING_PARAMETER, EStatCmd.receive_GyRAR_request);
            throw new MissingParameterException("", EError.GYRAR_MISSING_PARAMETER);
        }

        if (gyRARRequest == null) {
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.WRONG_FORMAT, EStatCmd.receive_GyRAR_request);
            throw new WrongFormatException("", EError.GYRAR_WRONG_FORMAT);
        }
    }

    public static void validateRefundManagement(RefundManagementRequest refundManagementRequest, AbstractAF abstractAF) throws MissingParameterException, WrongFormatException {

        if (refundManagementRequest == null) {
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.MISSING_PARAMETER, EStatCmd.Receive_Refund_Management_Request);
            throw new MissingParameterException("", EError.REFUND_MANAGEMENT_MISSING_PARAMETER);
        }

        if (refundManagementRequest == null) {
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.WRONG_FORMAT, EStatCmd.Receive_Refund_Management_Request);
            throw new WrongFormatException("", EError.REFUND_MANAGEMENT_WRONG_FORMAT);
        }
    }

    public static void validateRefundTransaction(RefundTransactionResponse refundTransactionResponse, AbstractAF abstractAF) throws MissingParameterException, WrongFormatException {

        if (refundTransactionResponse == null) {
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.MISSING_PARAMETER, EStatCmd.receive_Refund_Transaction_response);
            throw new MissingParameterException("", EError.REFUND_TRANSACTION_RESPONSE_MISSING_PARAMETER);
        }

        if (refundTransactionResponse == null) {
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.WRONG_FORMAT, EStatCmd.receive_Refund_Transaction_response);
            throw new WrongFormatException("", EError.REFUND_TRANSACTION_RESPONSE_WRONG_FORMAT);
        }
    }


}
