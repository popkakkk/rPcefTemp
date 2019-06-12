package phoebe.eqx.pcef.core;

import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import phoebe.eqx.pcef.core.exceptions.ExtractErrorException;
import phoebe.eqx.pcef.enums.EError;
import phoebe.eqx.pcef.enums.Operation;
import phoebe.eqx.pcef.message.builder.res.RefundManagementResponse;
import phoebe.eqx.pcef.message.parser.req.GyRARRequest;
import phoebe.eqx.pcef.message.parser.req.RefundManagementRequest;
import phoebe.eqx.pcef.message.parser.res.OCFUsageMonitoringResponse;
import phoebe.eqx.pcef.instance.TestResponseData;
import phoebe.eqx.pcef.message.parser.req.UsageMonitoringRequest;
import phoebe.eqx.pcef.message.parser.res.RefundTransactionResponse;
import phoebe.eqx.pcef.message.parser.res.product.GetResourceIdResponse;
import phoebe.eqx.pcef.utils.erd.ERDData;
import phoebe.eqx.pcef.utils.erd.ERDHeader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;

public class PCEFParser {
    private String message;
    private Gson gson = new Gson();

    public PCEFParser(String message) {
        this.message = message;
    }

    public TestResponseData translateTestResponseData() {
        return new TestResponseData();
    }

    public OCFUsageMonitoringResponse translateOCFUsageMonitoringResponse(Operation operation) throws ExtractErrorException {
        try {
            return gson.fromJson(message, OCFUsageMonitoringResponse.class);
        } catch (Exception e) {
            String errMeg = ExceptionUtils.getStackTrace(e);
            EError eError = null;
            if (operation.equals(Operation.UsageMonitoringStart)) {
                eError = EError.USAGE_MONITORING_START_RESPONSE_EXTRACT_ERROR;
            } else if (operation.equals(Operation.UsageMonitoringUpdate)) {
                eError = EError.USAGE_MONITORING_UPDATE_RESPONSE_EXTRACT_ERROR;
            } else if (operation.equals(Operation.UsageMonitoringStop)) {
                eError = EError.USAGE_MONITORING_STOP_RESPONSE_EXTRACT_ERROR;
            }
            throw new ExtractErrorException(errMeg, eError);
        }
    }

    public RefundTransactionResponse translateRefundTransactionResponse() throws ExtractErrorException {
        try {
            return gson.fromJson(message, RefundTransactionResponse.class);
        } catch (Exception e) {
            throw new ExtractErrorException(ExceptionUtils.getStackTrace(e), EError.REFUND_TRANSACTION_RESPONSE_EXTRACT_ERROR);
        }
    }

    public GetResourceIdResponse translateGetResourceId() throws ExtractErrorException {
        try {
            JAXBContext dataContext = JAXBContext.newInstance(ERDData.class);
            Unmarshaller dataUnMarshaller = dataContext.createUnmarshaller();

            String erdStr = message.substring(message.indexOf("<ERDData"));

            ERDData erdData = (ERDData) dataUnMarshaller.unmarshal(new StringReader(erdStr));
            return gson.fromJson(erdData.getValue(), GetResourceIdResponse.class);
        } catch (Exception e) {
            throw new ExtractErrorException(ExceptionUtils.getStackTrace(e), EError.GET_PRODUCT_RESPONSE_EXTRACT_ERROR);
        }
    }

    public UsageMonitoringRequest translateUsageMonitoringRequest() throws ExtractErrorException {
        try {
//            throw new Exception();
            return gson.fromJson(message, UsageMonitoringRequest.class);
        } catch (Exception e) {
            throw new ExtractErrorException(ExceptionUtils.getStackTrace(e), EError.USAGE_MONITORING_EXTRACT_ERROR);
        }
    }

    public GyRARRequest translateGyRARRequest() throws ExtractErrorException {
        try {
            return gson.fromJson(message, GyRARRequest.class);
        } catch (Exception e) {
            throw new ExtractErrorException(ExceptionUtils.getStackTrace(e), EError.GYRAR_EXTRACT_ERROR);
        }
    }

    public RefundManagementRequest translateRefundManagementRequest() throws ExtractErrorException {
        try {
            return gson.fromJson(message, RefundManagementRequest.class);
        } catch (Exception e) {
            throw new ExtractErrorException(ExceptionUtils.getStackTrace(e), EError.REFUND_MANAGEMENT_EXTRACT_ERROR);
        }
    }


}
