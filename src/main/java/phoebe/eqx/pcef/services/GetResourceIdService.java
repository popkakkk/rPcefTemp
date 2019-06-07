package phoebe.eqx.pcef.services;

import ec02.af.utils.AFLog;
import ec02.data.interfaces.EquinoxRawData;
import phoebe.eqx.pcef.core.exceptions.*;
import phoebe.eqx.pcef.enums.EError;
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
import phoebe.eqx.pcef.utils.WriteLog;
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
        return "PCEF" + PCEFUtils.regularDateFormat.format(appInstance.getMyContext().getStartTime());
    }

    public void buildGetResourceId() throws PCEFException {
        try {
            AFLog.d("Build Get ResourceResponse ID Request ..");

            String resourceName = context.getPcefInstance().getUsageMonitoringRequest().getResourceName();
            AFLog.d("resourceName : " + resourceName);

            //create invokeId
            String invokeId = generateInvokeId(Operation.GetResourceId);

            // logic build
            StringWriter erdHeaderWriter = new StringWriter();

            try {
                JAXBContext jaxbContext = JAXBContext.newInstance(ERDHeader.class);
                Marshaller marshaller = jaxbContext.createMarshaller();
                marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);


                String url = Config.URL_PRODUCT + "?category=IoT&page=1&limit=20&sort=id&order=desc" + "&keyword=" + resourceName;
                AFLog.d("url = " + url);

                List<Header> headers = Arrays.asList(
                        new Header("Content-Type", "application/json"),
                        new Header("x-app-name", "Singularity"),
                        new Header("x-channel", "WEB"),
                        new Header("x-transaction-id", generateTransactionId()), //gen to get product
                        new Header("x-auth-role", "SGL-00003"),
                        new Header("x-user-request", context.getPcefInstance().getUsageMonitoringRequest().getUserValue())
                );

                ERDHeader erdHeader = new ERDHeader();
                erdHeader.setHeaders(headers);
                marshaller.marshal(erdHeader, erdHeaderWriter);

                //build message
                MessagePool messagePool = new MessagePool(abstractAF);
                EquinoxRawData equinoxRawData = messagePool.getResourceIdRequest(erdHeaderWriter.toString(), invokeId, url);

                //add raw data to list
                invokeExternal(equinoxRawData, Operation.GetResourceId, messagePool.getRequestObj());

            } catch (Exception e) {
                throw new PCEFException(e.getStackTrace()[0].toString(), EError.GET_PRODUCT_BUILD_REQUEST_ERROR);
            }

            PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.sent_Get_Product_request);
            PCEFUtils.writeMessageFlow("Build Get ResourceResponse ID Request", MessageFlow.Status.Success, context.getPcefInstance().getSessionId());
        } catch (PCEFException e) {
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.ERROR, EStatCmd.sent_Get_Product_request);
            PCEFUtils.writeMessageFlow("Build Get ResourceResponse ID Request", MessageFlow.Status.Error, context.getPcefInstance().getSessionId());
            context.setPcefException(e);
            throw e;
        }
    }


    public void readGetResourceId() throws PCEFException {
        try {
            try {
                AFLog.d("Read Get ResourceResponse ID Response ..");
                String resourceId;

                Operation operation = Operation.GetResourceId;
                GetResourceIdResponse getResourceIdResponse = (GetResourceIdResponse) extractResponse(operation);

                ValidateMessage.validateGetResourceIdResponse(getResourceIdResponse, abstractAF);

                //summarylog res
//            context.setSummaryLogExternalResponse(operation, SummaryLog.getSummaryLogResponse(operation, testResponseData));

                resourceId = getResourceIdResponse.getResultData().getProducts().get(0).getProductId(); // get index = 0
                appInstance.getMyContext().getPcefInstance().setResourceId(resourceId);

                PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.receive_Get_Product_response);
                PCEFUtils.writeMessageFlow("Read Get ResourceResponse ID Response :" + resourceId, MessageFlow.Status.Success, context.getPcefInstance().getSessionId());
            } catch (TimeoutException e) {
                PCEFUtils.increaseStatistic(abstractAF, EStatMode.TIMEOUT, EStatCmd.receive_Get_Product_response);
                e.setError(EError.GET_PRODUCT_RESPONSE_TIMEOUT);
                throw e;
            } catch (ResponseErrorException e) {
                PCEFUtils.increaseStatistic(abstractAF, EStatMode.EQUINOX_ERROR, EStatCmd.receive_Get_Product_response);
                e.setError(EError.GET_PRODUCT_RESPONSE_EQUINOX_ERROR);
                throw e;
            }
        } catch (PCEFException e) {
            context.setPcefException(e);
            //summarylog fail
            PCEFUtils.writeMessageFlow("Read Get ResourceResponse ID Response", MessageFlow.Status.Error, context.getPcefInstance().getSessionId());
            throw e;
        }


    }


}
