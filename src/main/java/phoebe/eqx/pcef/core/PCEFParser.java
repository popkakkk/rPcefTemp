package phoebe.eqx.pcef.core;

import com.google.gson.Gson;
import phoebe.eqx.pcef.core.exceptions.ExtractErrorException;
import phoebe.eqx.pcef.enums.EError;
import phoebe.eqx.pcef.message.builder.res.RefundManagementResponse;
import phoebe.eqx.pcef.message.parser.req.GyRARRequest;
import phoebe.eqx.pcef.message.parser.req.RefundManagementRequest;
import phoebe.eqx.pcef.message.parser.res.OCFUsageMonitoringResponse;
import phoebe.eqx.pcef.instance.TestResponseData;
import phoebe.eqx.pcef.message.parser.req.UsageMonitoringRequest;
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

    public OCFUsageMonitoringResponse translateUsageMonitoringResponse() throws ExtractErrorException {
        try {
            return gson.fromJson(message, OCFUsageMonitoringResponse.class);
        } catch (Exception e) {
            throw new ExtractErrorException(e.getStackTrace()[0].toString(), EError.USAGE_MONITORING_EXTRACT_ERROR);
        }
    }

    public RefundManagementResponse translateRefundTransactionResponse() throws ExtractErrorException {
        try {
            return gson.fromJson(message, RefundManagementResponse.class);
        } catch (Exception e) {
            throw new ExtractErrorException(e.getStackTrace()[0].toString(), EError.USAGE_MONITORING_EXTRACT_ERROR);
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
            throw new ExtractErrorException(e.getStackTrace()[0].toString(), EError.USAGE_MONITORING_EXTRACT_ERROR);
        }
    }

    public UsageMonitoringRequest translateUsageMonitoringRequest() throws ExtractErrorException {
        try {
//            throw new Exception();
            return gson.fromJson(message, UsageMonitoringRequest.class);
        } catch (Exception e) {
            throw new ExtractErrorException(e.getStackTrace()[0].toString(), EError.USAGE_MONITORING_EXTRACT_ERROR);
        }
    }

    public GyRARRequest translateGyRARRequest() throws ExtractErrorException {
        try {
            return gson.fromJson(message, GyRARRequest.class);
        } catch (Exception e) {
            throw new ExtractErrorException(e.getStackTrace()[0].toString(), EError.USAGE_MONITORING_EXTRACT_ERROR);
        }
    }

    public RefundManagementRequest translateRefundManagementRequest() throws ExtractErrorException {
        try {
            return gson.fromJson(message, RefundManagementRequest.class);
        } catch (Exception e) {
            throw new ExtractErrorException(e.getStackTrace()[0].toString(), EError.USAGE_MONITORING_EXTRACT_ERROR);
        }
    }


}
