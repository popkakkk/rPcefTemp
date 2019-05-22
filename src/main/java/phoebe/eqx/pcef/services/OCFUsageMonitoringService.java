package phoebe.eqx.pcef.services;

import ec02.af.utils.AFLog;
import ec02.data.interfaces.EquinoxRawData;
import phoebe.eqx.pcef.message.parser.res.OCFUsageMonitoringResponse;
import phoebe.eqx.pcef.core.data.ResourceRequest;
import phoebe.eqx.pcef.core.exceptions.ResponseErrorException;
import phoebe.eqx.pcef.core.exceptions.TimeoutException;
import phoebe.eqx.pcef.core.model.Quota;
import phoebe.eqx.pcef.core.model.Transaction;
import phoebe.eqx.pcef.enums.Operation;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.instance.PCEFInstance;
import phoebe.eqx.pcef.message.builder.MessagePool;
import phoebe.eqx.pcef.message.builder.req.OCFUsageMonitoringRequest;
import phoebe.eqx.pcef.utils.MessageFlow;
import phoebe.eqx.pcef.utils.PCEFUtils;
import phoebe.eqx.pcef.utils.ValidateMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OCFUsageMonitoringService extends PCEFService {
    public OCFUsageMonitoringService(AppInstance appInstance) {
        super(appInstance);
    }


    public boolean receiveQuotaAndPolicy(OCFUsageMonitoringResponse OCFUsageMonitoringResponse) {

        List<String> resourceIdResponseList = new ArrayList<>();

        OCFUsageMonitoringResponse.getResources().forEach(resourceResponse -> resourceIdResponseList.add(resourceResponse.getResourceId()));

        List<ResourceRequest> resourceRequestList = appInstance.getPcefInstance().getOcfUsageMonitoringRequest().getResources();
        for (ResourceRequest resourceRequest : resourceRequestList) {
            if (appInstance.getPcefInstance().isQuotaExhaust()) {
                String rr = resourceRequest.getReportingReason();
                if (rr.equals("1")) {
                    continue;
                }
            }
            if (!resourceIdResponseList.contains(resourceRequest.getResourceId())) {
                AFLog.d("resourceId:"+resourceRequest.getResourceId() + "dont have quota receive");
                return false;
            }
        }
        return true;
    }


    private String generateSessionId() {
        return "sessionIdFromPCEF";
    }

    private String generateTransactionId() {
        return "transactionId" + appInstance.getPcefInstance().getSequenceNumber();
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
        Transaction transaction = pcefInstance.getTransaction();

        umRequest.setActualTime(transaction.getActualTime());
        umRequest.setUserType(transaction.getUserType());
        umRequest.setUserValue(transaction.getUserValue());

        pcefInstance.increasSequenceNumber();
        umRequest.setTid(generateTransactionId());
        umRequest.setRequestNumber(String.valueOf(pcefInstance.getSequenceNumber()));


        //index(0) must is this transaction
        transactionList.addAll(appInstance.getPcefInstance().getOtherStartTransactions());


        //set all resource initial
        List<String> resourceIdList = new ArrayList<>();
        for (Transaction t : transactionList) {
            String resourceId = t.getResourceId();
            if (!resourceIdList.contains(resourceId)) {
                ResourceRequest resourceRequest = new ResourceRequest();
                resourceRequest.setResourceId(resourceId);
                resourceRequest.setResourceName(t.getResourceName());
                resourceRequest.setRtid(t.getRtid());

                //add resources
                umRequest.getResources().add(resourceRequest);

                //for check duplicate resourceId
                resourceIdList.add(resourceId);
            }

        }

        appInstance.getPcefInstance().setOcfUsageMonitoringRequest(umRequest);

        MessagePool messagePool = new MessagePool(abstractAF);
        EquinoxRawData equinoxRawData = messagePool.getUsageMonitoringStartRequest(umRequest, invokeId);
        invokeExternal(equinoxRawData, operation, messagePool.getRequestObj());
    }


    private ArrayList<ResourceRequest> getResourceCommitRequest(Quota quotaCommit, Map<String, Integer> countUnitMap) {
        ArrayList<ResourceRequest> resourceRequestList = new ArrayList<>();
        quotaCommit.getResources().forEach(resourceQuota -> {
            ResourceRequest resourceRequest = new ResourceRequest();
            resourceRequest.setResourceId(resourceQuota.getResourceId());
            resourceRequest.setResourceName(resourceQuota.getResourceName());
            resourceRequest.setMonitoringKey(quotaCommit.getMonitoringKey());
            resourceRequest.setRtid(appInstance.getPcefInstance().getTransaction().getRtid());
            resourceRequest.setUnitType("unit");
            resourceRequest.setUsedUnit(String.valueOf(countUnitMap.get(resourceQuota.getResourceId())));
            resourceRequest.setReportingReason("0");
            resourceRequestList.add(resourceRequest);
        });
        return resourceRequestList;
    }

    public void buildUsageMonitoringUpdate() {
        try {
            PCEFInstance pcefInstance = appInstance.getPcefInstance();
            Operation operation = Operation.UsageMonitoringUpdate;
            String invokeId = "umUpdate_";

            OCFUsageMonitoringRequest umStartRequest = new OCFUsageMonitoringRequest();
            umStartRequest.setCommand("usageMonitoringUpdate");
            umStartRequest.setSessionId(pcefInstance.getOcfSessionId());


            List<Transaction> transactionStartList = new ArrayList<>();
            if (pcefInstance.isQuotaExhaust()) {//update commit
                //set unit to Resource
                Quota quotaCommit = pcefInstance.getQuotaToCommit();
                Map<String, Integer> countUnitMap = pcefInstance.getCountUnitMap();
                umStartRequest.setResources(getResourceCommitRequest(quotaCommit, countUnitMap));
            } else {
                //update initial
                transactionStartList.add(pcefInstance.getTransaction());
            }

            buildUsageMonitoring(umStartRequest, operation, invokeId, transactionStartList);
            PCEFUtils.writeMessageFlow("Build Usage Monitoring Update Request", MessageFlow.Status.Success, appInstance.getPcefInstance().getSessionId());
        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Build Usage Monitoring Update Request", MessageFlow.Status.Error, appInstance.getPcefInstance().getSessionId());
        }
    }


    public OCFUsageMonitoringResponse readUsageMonitoringStart() throws Exception {

        try {
            Operation operation = Operation.UsageMonitoringStart;

            //extract
            OCFUsageMonitoringResponse OCFUsageMonitoringResponse = (OCFUsageMonitoringResponse) extractResponse(operation);
//            this.appInstance.getPcefInstance().setUsageMonitoring(OCFUsageMonitoringResponse);

            //validate
            ValidateMessage.validateTestData();

            //increase stat success
//            PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.PCEF_RECEIVE_TEST_DATA);

            //summarylog res
//            appInstance.setSummaryLogExternalResponse(Operation.TestOperation, SummaryLog.getSummaryLogResponse(Operation.TestOperation, testResponseData));

            PCEFUtils.writeMessageFlow("Read Usage Monitoring Start Response", MessageFlow.Status.Success, appInstance.getPcefInstance().getSessionId());
            return OCFUsageMonitoringResponse;
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

    public OCFUsageMonitoringResponse readUsageMonitoringUpdate() throws Exception {

        try {
            Operation operation = Operation.UsageMonitoringUpdate;

            //extract
            OCFUsageMonitoringResponse OCFUsageMonitoringResponse = (OCFUsageMonitoringResponse) extractResponse(operation);
//            this.appInstance.getPcefInstance().setUsageMonitoring(OCFUsageMonitoringResponse);

            //validate
            ValidateMessage.validateTestData();

            //increase stat success
//            PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.PCEF_RECEIVE_TEST_DATA);

            //summarylog res
//            appInstance.setSummaryLogExternalResponse(Operation.TestOperation, SummaryLog.getSummaryLogResponse(Operation.TestOperation, testResponseData));

            PCEFUtils.writeMessageFlow("Read Usage Monitoring Update Response", MessageFlow.Status.Success, appInstance.getPcefInstance().getSessionId());
            return OCFUsageMonitoringResponse;
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
