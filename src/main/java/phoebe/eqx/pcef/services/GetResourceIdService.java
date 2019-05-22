package phoebe.eqx.pcef.services;

import ec02.data.interfaces.EquinoxRawData;
import phoebe.eqx.pcef.core.exceptions.ResponseErrorException;
import phoebe.eqx.pcef.core.exceptions.TimeoutException;
import phoebe.eqx.pcef.enums.Operation;
import phoebe.eqx.pcef.enums.stats.EStatCmd;
import phoebe.eqx.pcef.enums.stats.EStatMode;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.instance.Config;
import phoebe.eqx.pcef.message.builder.MessagePool;
import phoebe.eqx.pcef.message.parser.res.product.GetResourceIdResponse;
import phoebe.eqx.pcef.utils.MessageFlow;
import phoebe.eqx.pcef.utils.PCEFUtils;
import phoebe.eqx.pcef.utils.ValidateMessage;
import phoebe.eqx.pcef.utils.erd.ERDData;
import phoebe.eqx.pcef.utils.erd.ERDHeader;
import phoebe.eqx.pcef.utils.erd.Header;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

public class GetResourceIdService extends PCEFService {

    public GetResourceIdService(AppInstance appInstance) {
        super(appInstance);
    }


    private String generateTransactionId() {
        return "t-" + PCEFUtils.getDate(0).getTime() + PCEFUtils.randomNumber3Digit();
    }

    public void buildGetResourceId() {
        try {
            //create invokeId
            String invokeId = "getResourceId_" /*+ PCEFUtils.getDate(0).getTime() + PCEFUtils.randomNumber3Digit()*/;

            // logic build
            StringWriter erdHeaderWriter = new StringWriter();

            JAXBContext context = JAXBContext.newInstance(ERDHeader.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);


            String url = Config.URL_PRODUCT + "?category=IoT&page=1&limit=20&sort=id&order=desc" + "&keyword=" + appInstance.getPcefInstance().getUsageMonitoringRequest().getResourceName();


            List<Header> headers = Arrays.asList(
                    new Header("Content-Type", "application/json"),
                    new Header("x-app-name", "Singularity"),
                    new Header("x-channel", "WEB"),
                    new Header("x-transaction-id", generateTransactionId()), //gen to product
                    new Header("x-auth-role", "SI"),
                    new Header("x-user-request", appInstance.getPcefInstance().getUsageMonitoringRequest().getUserValue())
            );

            ERDHeader erdHeader = new ERDHeader();
            erdHeader.setHeaders(headers);
            marshaller.marshal(erdHeader, erdHeaderWriter);

            //build message
            MessagePool messagePool = new MessagePool(abstractAF);
            EquinoxRawData equinoxRawData = messagePool.getResourceIdRequest(erdHeaderWriter.toString(), invokeId,url);

            //add raw data to list
            invokeExternal(equinoxRawData, Operation.GetResourceId, messagePool.getRequestObj());

            //increase stat
//            PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.PCEF_RECEIVE_TEST_DATA);

            PCEFUtils.writeMessageFlow("Build Get ResourceResponse ID Request", MessageFlow.Status.Success, appInstance.getPcefInstance().getSessionId());
        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Build Get ResourceResponse ID Request", MessageFlow.Status.Error, appInstance.getPcefInstance().getSessionId());
        }
    }


    public String readGetResourceId() {
        String resourceId = null;
        try {
            //extract
            Operation operation = Operation.GetResourceId;

            GetResourceIdResponse getResourceIdResponse = (GetResourceIdResponse) extractResponse(operation);


            //validate
            ValidateMessage.validateTestData();

            //increase stat success
//            PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.PCEF_RECEIVE_TEST_DATA);

            //summarylog res
//            appInstance.setSummaryLogExternalResponse(operation, SummaryLog.getSummaryLogResponse(operation, testResponseData));

            resourceId = getResourceIdResponse.getResultData().getProducts().get(0).getProductId(); // get index = 0

            PCEFUtils.writeMessageFlow("Read Get ResourceResponse ID Response :" + resourceId, MessageFlow.Status.Success, appInstance.getPcefInstance().getSessionId());
        } catch (TimeoutException e) {
            // handle time out
        } catch (ResponseErrorException e) {
            // handle ret error
        } catch (Exception e) {
            //increase stat fail
            //summarylog fail
            // read fail
            PCEFUtils.writeMessageFlow("Read Get ResourceResponse ID Response", MessageFlow.Status.Error, appInstance.getPcefInstance().getSessionId());

        }

        return resourceId;
    }


}
