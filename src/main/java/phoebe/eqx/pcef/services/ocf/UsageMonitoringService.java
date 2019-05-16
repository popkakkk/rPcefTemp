package phoebe.eqx.pcef.services.ocf;

import ec02.data.interfaces.EquinoxRawData;
import phoebe.eqx.pcef.core.data.ResourceRequest;
import phoebe.eqx.pcef.core.data.UsageMonitoring;
import phoebe.eqx.pcef.core.data.ResourceResponse;
import phoebe.eqx.pcef.core.exceptions.ResponseErrorException;
import phoebe.eqx.pcef.core.exceptions.TimeoutException;
import phoebe.eqx.pcef.core.model.Quota;
import phoebe.eqx.pcef.enums.Operation;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.instance.PCEFInstance;
import phoebe.eqx.pcef.message.builder.MessagePool;
import phoebe.eqx.pcef.message.builder.req.OCFUsageMonitoringRequest;
import phoebe.eqx.pcef.core.model.Transaction;
import phoebe.eqx.pcef.services.PCEFService;
import phoebe.eqx.pcef.utils.MessageFlow;
import phoebe.eqx.pcef.utils.PCEFUtils;
import phoebe.eqx.pcef.utils.ValidateMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class UsageMonitoringService extends PCEFService {
    public UsageMonitoringService(AppInstance appInstance) {
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

            OCFUsageMonitoringRequest umStartRequest = new OCFUsageMonitoringRequest();
            umStartRequest.setCommand("usageMonitoringStart");

            String sessionId = generateSessionId();
            appInstance.getPcefInstance().setOcfSessionId(sessionId);
            umStartRequest.setSessionId(sessionId);//generate

            umStartRequest.setTid(generateTransactionId()); //generate
            umStartRequest.setRequestNumber("1"); // #1 = start

            List<Transaction> transactionList = new ArrayList<>();
            transactionList.add(appInstance.getPcefInstance().getTransaction());

            buildUsageMonitoring(umStartRequest, operation, invokeId, transactionList);

            PCEFUtils.writeMessageFlow("Build Usage Monitoring Start Request", MessageFlow.Status.Success, appInstance.getPcefInstance().getSessionId());
        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Build Usage Monitoring Start Request", MessageFlow.Status.Error, appInstance.getPcefInstance().getSessionId());
        }
    }

    private void buildUsageMonitoring(OCFUsageMonitoringRequest umRequest, Operation operation, String invokeId, List<Transaction> transactionList) throws Exception {

        PCEFInstance pcefInstance = appInstance.getPcefInstance();
        Transaction transaction = pcefInstance.getOtherStartTransactions().get(0);

        umRequest.setActualTime(transaction.getActualTime());
        umRequest.setUserType(transaction.getUserType());
        umRequest.setUserValue(transaction.getUserValue());

        transactionList.addAll(appInstance.getPcefInstance().getOtherStartTransactions());

        Collections.copy(transactionList, pcefInstance.getOtherStartTransactions());
        if (!pcefInstance.isQuotaExhaust()) {
            transactionList.add(transaction);
        }

        //set all resource initial
        transactionList.forEach(t -> {
            ResourceRequest resourceRequest = new ResourceRequest();
            resourceRequest.setResourceId(t.getResourceId());
            resourceRequest.setResourceName(t.getResourceName());
            resourceRequest.setRtid(t.getRtid());
            umRequest.getResourceRequests().add(resourceRequest);
        });


        MessagePool messagePool = new MessagePool(abstractAF);
        EquinoxRawData equinoxRawData = messagePool.getUsageMonitoringStartRequest(umRequest, invokeId);
        invokeExternal(equinoxRawData, operation, messagePool.getRequestObj());
    }


    private ArrayList<ResourceRequest> getResourceCommitRequest(Quota quotaCommit, Map<String, String> countUnitMap) {
        ArrayList<ResourceRequest> resourceRequestList = new ArrayList<>();
        quotaCommit.getResources().forEach(resourceQuota -> {
            ResourceRequest resourceRequest = new ResourceRequest();
            resourceRequest.setResourceId(resourceQuota.getResourceId());
            resourceRequest.setResourceName(resourceQuota.getResourceName());
            resourceRequest.setMonitoringKey(quotaCommit.getMonitoringKey());
            resourceRequest.setRtid(appInstance.getPcefInstance().getTransaction().getRtid());
            resourceRequest.setUnitType("unit");
            resourceRequest.setUsedUnit(countUnitMap.get(resourceQuota.getResourceId()));
            resourceRequestList.add(resourceRequest);
        });
        return resourceRequestList;
    }

    public void buildUsageMonitoringUpdate() {
        try {
            Operation operation = Operation.UsageMonitoringUpdate;
            String invokeId = "umUpdate_";

            OCFUsageMonitoringRequest umStartRequest = new OCFUsageMonitoringRequest();
            umStartRequest.setCommand("usageMonitoringUpdate");
            umStartRequest.setSessionId(appInstance.getPcefInstance().getOcfSessionId());
            umStartRequest.setTid(generateTransactionId());
//                umStartRequest.setRequestNumber("1"); //

            List<Transaction> transactionStartList = new ArrayList<>();
            if (appInstance.getPcefInstance().isQuotaExhaust()) {
                //update commit
                Quota quotaCommit = appInstance.getPcefInstance().getQuotaCommit();
                Map<String, String> countUnitMap = appInstance.getPcefInstance().getCountUnitMap();
                umStartRequest.setResourceRequests(getResourceCommitRequest(quotaCommit, countUnitMap));
            } else {
                //update initial
                transactionStartList.add(appInstance.getPcefInstance().getTransaction());
            }

            buildUsageMonitoring(umStartRequest, operation, invokeId, transactionStartList);
            PCEFUtils.writeMessageFlow("Build Usage Monitoring Update Request", MessageFlow.Status.Success, appInstance.getPcefInstance().getSessionId());
        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Build Usage Monitoring Update Request", MessageFlow.Status.Error, appInstance.getPcefInstance().getSessionId());
        }
    }


    public UsageMonitoring readUsageMonitoringStart() throws Exception {

        try {
            Operation operation = Operation.UsageMonitoringStart;

            //extract
            UsageMonitoring usageMonitoring = (UsageMonitoring) extractResponse(operation);
//            this.appInstance.getPcefInstance().setUsageMonitoring(UsageMonitoring);

            //validate
            ValidateMessage.validateTestData();

            //increase stat success
//            PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.PCEF_RECEIVE_TEST_DATA);

            //summarylog res
//            appInstance.setSummaryLogExternalResponse(Operation.TestOperation, SummaryLog.getSummaryLogResponse(Operation.TestOperation, testResponseData));

            PCEFUtils.writeMessageFlow("Read Usage Monitoring Start Response", MessageFlow.Status.Success, appInstance.getPcefInstance().getSessionId());
            return usageMonitoring;
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

    public UsageMonitoring readUsageMonitoringUpdate() throws Exception {

        try {
            Operation operation = Operation.UsageMonitoringUpdate;

            //extract
            UsageMonitoring usageMonitoring = (UsageMonitoring) extractResponse(operation);
//            this.appInstance.getPcefInstance().setUsageMonitoring(UsageMonitoring);

            //validate
            ValidateMessage.validateTestData();

            //increase stat success
//            PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.PCEF_RECEIVE_TEST_DATA);

            //summarylog res
//            appInstance.setSummaryLogExternalResponse(Operation.TestOperation, SummaryLog.getSummaryLogResponse(Operation.TestOperation, testResponseData));

            PCEFUtils.writeMessageFlow("Read Usage Monitoring Update Response", MessageFlow.Status.Success, appInstance.getPcefInstance().getSessionId());
            return usageMonitoring;
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
