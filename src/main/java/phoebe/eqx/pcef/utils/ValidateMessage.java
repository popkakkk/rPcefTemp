package phoebe.eqx.pcef.utils;

import org.apache.commons.lang3.StringUtils;
import phoebe.eqx.pcef.core.exceptions.MissingParameterException;
import phoebe.eqx.pcef.core.exceptions.WrongFormatException;
import phoebe.eqx.pcef.enums.EError;
import phoebe.eqx.pcef.message.parser.req.UsageMonitoringRequest;
import phoebe.eqx.pcef.message.parser.res.OCFUsageMonitoringResponse;
import phoebe.eqx.pcef.message.parser.res.product.GetResourceIdResponse;

public class ValidateMessage {

    public static void validateTestData() throws Exception {
        if (1 == 2) {
            throw new Exception("");
        }

    }


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

       /* if (usageMonitoringRequest == null) {
            throw new WrongFormatException("", EError.USAGE_MONITORING_WRONG_FORMAT);
        }*/


    }

    public static void validateGetResourceIdResponse(GetResourceIdResponse getResourceIdResponse) throws MissingParameterException, WrongFormatException {

        if (getResourceIdResponse == null) {
            throw new MissingParameterException("", EError.USAGE_MONITORING_MISSING_PARAMETER);
        }

        if (getResourceIdResponse == null) {
            throw new WrongFormatException("", EError.USAGE_MONITORING_WRONG_FORMAT);
        }

    }

    public static void validateUsageMonitoringStart(OCFUsageMonitoringResponse ocfUsageMonitoringResponse) throws MissingParameterException, WrongFormatException {

        if (ocfUsageMonitoringResponse == null) {
            throw new MissingParameterException("", EError.USAGE_MONITORING_MISSING_PARAMETER);
        }

        if (ocfUsageMonitoringResponse == null) {
            throw new WrongFormatException("", EError.USAGE_MONITORING_WRONG_FORMAT);
        }

    }

    public static void validateUsageMonitoringUpdate(OCFUsageMonitoringResponse ocfUsageMonitoringResponse) throws MissingParameterException, WrongFormatException {

        if (ocfUsageMonitoringResponse == null) {
            throw new MissingParameterException("", EError.USAGE_MONITORING_MISSING_PARAMETER);
        }

        if (ocfUsageMonitoringResponse == null) {
            throw new WrongFormatException("", EError.USAGE_MONITORING_WRONG_FORMAT);
        }

    }

    public static void validateUsageMonitoringStop(OCFUsageMonitoringResponse ocfUsageMonitoringResponse) throws MissingParameterException, WrongFormatException {

        if (ocfUsageMonitoringResponse == null) {
            throw new MissingParameterException("", EError.USAGE_MONITORING_MISSING_PARAMETER);
        }

        if (ocfUsageMonitoringResponse == null) {
            throw new WrongFormatException("", EError.USAGE_MONITORING_WRONG_FORMAT);
        }

    }


}
