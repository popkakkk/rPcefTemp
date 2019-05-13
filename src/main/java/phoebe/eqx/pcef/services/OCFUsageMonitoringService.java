package phoebe.eqx.pcef.services;

import ec02.data.interfaces.EquinoxRawData;
import phoebe.eqx.pcef.core.data.OCFUsageMonitoring;
import phoebe.eqx.pcef.core.data.Resource;
import phoebe.eqx.pcef.core.exceptions.ResponseErrorException;
import phoebe.eqx.pcef.core.exceptions.TimeoutException;
import phoebe.eqx.pcef.enums.Operation;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.message.builder.MessagePool;
import phoebe.eqx.pcef.message.builder.req.UsageMonitoringStartRequest;
import phoebe.eqx.pcef.model.Transaction;
import phoebe.eqx.pcef.utils.MessageFlow;
import phoebe.eqx.pcef.utils.PCEFUtils;
import phoebe.eqx.pcef.utils.ValidateMessage;

import java.util.ArrayList;
import java.util.List;

public class OCFUsageMonitoringService extends PCEFService {
    public OCFUsageMonitoringService(AppInstance appInstance) {
        super(appInstance);
    }


    public boolean receiveQuotaAndPolicy() {
        return true;
    }


    private String generateSessionId() {
        return "sessionIdFromPCEF";
    }

    private String generateTransactionId() {
        return "transactionIdFromPCEF";
    }


    public void buildUsageMonitoringStart() {
        try {
            Operation operation = Operation.UsageMonitoringStart;
            String invokeId = "umStart_";

            Transaction transaction = appInstance.getPcefInstance().getTransactions().get(0);

            UsageMonitoringStartRequest umStartRequest = new UsageMonitoringStartRequest();
            umStartRequest.setCommand("usageMonitoringStart");
            umStartRequest.setSessionId(generateSessionId());//generate
            umStartRequest.setTid(generateTransactionId()); //generate
            umStartRequest.setRequestNumber("1"); // #1 = start
            umStartRequest.setActualTime(transaction.getActualTime());
            umStartRequest.setUserType(transaction.getUserType());
            umStartRequest.setUserValue(transaction.getUserValue());


            //set all resource
            appInstance.getPcefInstance().getTransactions().forEach(t -> {
                Resource resource = new Resource();
                resource.setResourceId(t.getResourceId());
                resource.setResourceName(t.getResourceName());
                resource.setRtid(t.getRtid());
                umStartRequest.getResources().add(resource);
            });


            MessagePool messagePool = new MessagePool(abstractAF);
            EquinoxRawData equinoxRawData = messagePool.getUsageMonitoringStartRequest(umStartRequest, invokeId);
            invokeExternal(equinoxRawData, operation, messagePool.getRequestObj());

            PCEFUtils.writeMessageFlow("Build Usage Monitoring Start Request", MessageFlow.Status.Success, appInstance.getPcefInstance().getSessionId());
        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Build Usage Monitoring Start Request", MessageFlow.Status.Error, appInstance.getPcefInstance().getSessionId());
        }
    }

    public void buildUsageMonitoringUpdate() {
        try {


        } catch (Exception e) {

        }
    }

    public OCFUsageMonitoring readUsageMonitoringStart() throws Exception {

        try {
            Operation operation = Operation.UsageMonitoringStart;

            //extract
            OCFUsageMonitoring ocfUsageMonitoring = (OCFUsageMonitoring) extractResponse(operation);
//            this.appInstance.getPcefInstance().setOcfUsageMonitoring(OCFUsageMonitoring);

            //validate
            ValidateMessage.validateTestData();

            //increase stat success
//            PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.PCEF_RECEIVE_TEST_DATA);

            //summarylog res
//            appInstance.setSummaryLogExternalResponse(Operation.TestOperation, SummaryLog.getSummaryLogResponse(Operation.TestOperation, testResponseData));

            PCEFUtils.writeMessageFlow("Read Usage Monitoring Start Response", MessageFlow.Status.Success, appInstance.getPcefInstance().getSessionId());
            return ocfUsageMonitoring;
        } catch (TimeoutException e) {
            // handle time out
            throw e;
        } catch (ResponseErrorException e) {

            // handle ret error
            throw e;
        } catch (Exception e) {
            //increase stat fail
            //summarylog fail
            // read fail


            PCEFUtils.writeMessageFlow("Read Usage Monitoring Start Response", MessageFlow.Status.Error, appInstance.getPcefInstance().getSessionId());
            throw e;
        }

    }


}
