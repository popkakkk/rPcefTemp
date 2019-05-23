package phoebe.eqx.pcef.core;

import com.google.gson.Gson;
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

    public OCFUsageMonitoringResponse translateUsageMonitoringResponse() {
        return gson.fromJson(message, OCFUsageMonitoringResponse.class);
    }

    public GetResourceIdResponse translateGetResourceId() throws JAXBException {
        JAXBContext dataContext = JAXBContext.newInstance(ERDData.class);
        Unmarshaller dataUnMarshaller = dataContext.createUnmarshaller();

        String erdStr = message.substring(message.indexOf("<ERDData"));

        ERDData erdData = (ERDData) dataUnMarshaller.unmarshal(new StringReader(erdStr));
        return gson.fromJson(erdData.getValue(), GetResourceIdResponse.class);
    }

    public UsageMonitoringRequest translateUsageMonitoringRequest() {
        return gson.fromJson(message, UsageMonitoringRequest.class);
    }

    public GyRARRequest translateGyRARRequest() {
        return gson.fromJson(message, GyRARRequest.class);
    }

    public RefundManagementRequest translateRefundManagementRequest() {
        return gson.fromJson(message, RefundManagementRequest.class);
    }


}
