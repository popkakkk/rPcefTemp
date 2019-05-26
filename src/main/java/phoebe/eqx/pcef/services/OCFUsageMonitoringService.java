package phoebe.eqx.pcef.services;

import ec02.data.interfaces.EquinoxRawData;
import phoebe.eqx.pcef.core.data.ResourceQuota;
import phoebe.eqx.pcef.enums.ERequestType;
import phoebe.eqx.pcef.instance.CommitData;
import phoebe.eqx.pcef.message.parser.req.UsageMonitoringRequest;
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
import phoebe.eqx.pcef.services.mogodb.MongoDBConnect;
import phoebe.eqx.pcef.utils.MessageFlow;
import phoebe.eqx.pcef.utils.PCEFUtils;
import phoebe.eqx.pcef.utils.ValidateMessage;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class OCFUsageMonitoringService extends PCEFService {
    public OCFUsageMonitoringService(AppInstance appInstance) {
        super(appInstance);
    }


    public boolean receiveQuotaAndPolicy(OCFUsageMonitoringResponse OCFUsageMonitoringResponse) {

        /*List<String> resourceIdResponseList = new ArrayList<>();

        OCFUsageMonitoringResponse.getResources().forEach(resourceResponse -> resourceIdResponseList.add(resourceResponse.getResourceId()));

        List<ResourceRequest> resourceRequestList = appInstance.getPcefInstance().getOcfUsageMonitoringRequest().getResources();
        for (ResourceRequest resourceRequest : resourceRequestList) {
            if (appInstance.getPcefInstance().doCommit()) {
                String rr = resourceRequest.getReportingReason();
                if (rr.equals("1")) {
                    continue;
                }
            }
            if (!resourceIdResponseList.contains(resourceRequest.getResourceId())) {
                AFLog.d("resourceId:" + resourceRequest.getResourceId() + "dont have quota receive");
                return false;
            }
        }*/
        return true;
    }


    private String generateSessionId() {
        return "sessionIdFromPCEF";
    }

    private String generateTransactionId() {
        return "transactionId" + appInstance.getPcefInstance().getProfile().getSequenceNumber();
    }


    public void buildUsageMonitoringStart(MongoDBConnect dbConnect) {
        try {

            //find other transaction
            List<Transaction> otherTransactionStartList = dbConnect.getTransactionService().findOtherStartTransaction(appInstance.getPcefInstance().getProfile().getUserValue());
            appInstance.getPcefInstance().getOtherStartTransactions().addAll(otherTransactionStartList);

            Operation operation = Operation.UsageMonitoringStart;
            String invokeId = "umStart_";

            OCFUsageMonitoringRequest umStartRequest = new OCFUsageMonitoringRequest();
            umStartRequest.setCommand("usageMonitoringStart");

            String sessionId = generateSessionId();
            appInstance.getPcefInstance().getProfile().setSessionId(sessionId);
            umStartRequest.setSessionId(sessionId);//generate
            umStartRequest.setActualTime(appInstance.getPcefInstance().getTransaction().getActualTime());

            List<Transaction> transactionList = new ArrayList<>();
            transactionList.add(appInstance.getPcefInstance().getTransaction());

            buildUsageMonitoring(umStartRequest, operation, invokeId, transactionList);

            PCEFUtils.writeMessageFlow("Build Usage Monitoring Start Request", MessageFlow.Status.Success, appInstance.getPcefInstance().getSessionId());
        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Build Usage Monitoring Start Request" + e.getStackTrace()[0], MessageFlow.Status.Error, appInstance.getPcefInstance().getSessionId());
        }
    }


    private void buildUsageMonitoring(OCFUsageMonitoringRequest umRequest, Operation operation, String invokeId, List<Transaction> transactionList) throws Exception {

        PCEFInstance pcefInstance = appInstance.getPcefInstance();


        umRequest.setUserType(pcefInstance.getProfile().getUserType());
        umRequest.setUserValue(pcefInstance.getProfile().getUserValue());

        pcefInstance.getProfile().increaseSequenceNumber();
        umRequest.setTid(generateTransactionId());
        umRequest.setRequestNumber(String.valueOf(pcefInstance.getProfile().getSequenceNumber()));

        transactionList.addAll(appInstance.getPcefInstance().getOtherStartTransactions());

        //set all new resource
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


        MessagePool messagePool = new MessagePool(abstractAF);
        EquinoxRawData equinoxRawData = messagePool.getOCFUsageMonitoringRequest(umRequest, invokeId);
        invokeExternal(equinoxRawData, operation, messagePool.getRequestObj());
    }


    private ArrayList<ResourceRequest> getResourceCommitExhaustRequest(Quota quotaExhaust, Map<String, Integer> countUnitMap) {
        ArrayList<ResourceRequest> resourceRequestList = new ArrayList<>();
        quotaExhaust.getResources().forEach(resourceQuota -> {
            ResourceRequest resourceRequest = new ResourceRequest();
            resourceRequest.setResourceId(resourceQuota.getResourceId());
            resourceRequest.setResourceName(resourceQuota.getResourceName());
            resourceRequest.setMonitoringKey(quotaExhaust.getMonitoringKey());
            resourceRequest.setRtid(appInstance.getPcefInstance().getTransaction().getRtid());
            resourceRequest.setUnitType("unit");
            resourceRequest.setUsedUnit(String.valueOf(countUnitMap.get(resourceQuota.getResourceId())));
            resourceRequest.setReportingReason("0");
            resourceRequestList.add(resourceRequest);
        });
        return resourceRequestList;
    }


    private ArrayList<ResourceRequest> getResourceCommitExpireRequest(List<Quota> quotaExpireList, Map<String, Integer> countUnitMap) {
        ArrayList<ResourceRequest> resourceRequestList = new ArrayList<>();

        for (Quota quota : quotaExpireList) {
            for (ResourceQuota resourceQuota : quota.getResources()) {
                ResourceRequest resourceRequest = new ResourceRequest();
                resourceRequest.setResourceId(resourceQuota.getResourceId());
                resourceRequest.setResourceName(resourceQuota.getResourceName());
                resourceRequest.setMonitoringKey(quota.getMonitoringKey());
//                resourceRequest.setRtid(appInstance.getPcefInstance().getTransaction().getRtid()); // last of tid
                resourceRequest.setUnitType("unit");

                int unit = countUnitMap.get(resourceQuota.getResourceId());
                resourceRequest.setUsedUnit(String.valueOf(unit));
                if (unit == 0) {
                    resourceRequest.setReportingReason("1");
                } else {
                    resourceRequest.setReportingReason("0");
                }
                resourceRequestList.add(resourceRequest);
            }
        }
        return resourceRequestList;
    }

    private ArrayList<ResourceRequest> getResourceCommit(List<CommitData> commitDataList, ERequestType requestType) {
        ArrayList<ResourceRequest> resourceRequestList = new ArrayList<>();

        for (CommitData commitData : commitDataList) {

            ResourceRequest resourceRequest = new ResourceRequest();
            resourceRequest.setResourceId(commitData.get_id().getResourceId());
            resourceRequest.setResourceName(commitData.get_id().getResourceName());
            resourceRequest.setMonitoringKey(commitData.get_id().getMonitoringKey());
            resourceRequest.setUnitType("unit");

            int unit = commitData.getCount();

            resourceRequest.setUsedUnit(String.valueOf(unit));

            if (ERequestType.USAGE_MONITORING.equals(requestType)) {
                resourceRequest.setRtid(appInstance.getPcefInstance().getTransaction().getRtid());
                resourceRequest.setReportingReason("0");
            } else if (ERequestType.E11_TIMEOUT.equals(requestType)) {
//                resourceRequest.setRtid(appInstance.getPcefInstance().getTransaction().getRtid()); // last of tid

                if (unit == 0) {
                    resourceRequest.setReportingReason("1");
                } else {
                    resourceRequest.setReportingReason("0");
                }

            } else if (ERequestType.GyRAR.equals(requestType)) {
                resourceRequest.setReportingReason("0");
            }
            resourceRequestList.add(resourceRequest);
        }


        return resourceRequestList;
    }

    private ArrayList<ResourceRequest> getResourceStopRequest(List<CommitData> commitDataList) {
        ArrayList<ResourceRequest> resourceRequestList = new ArrayList<>();
        for (CommitData commitData : commitDataList) {
            ResourceRequest resourceRequest = new ResourceRequest();
            resourceRequest.setResourceId(commitData.get_id().getResourceId());
            resourceRequest.setResourceName(commitData.get_id().getResourceName());
            resourceRequest.setMonitoringKey(commitData.get_id().getMonitoringKey());
            resourceRequest.setRtid("UNKNOWN");
            resourceRequest.setUnitType("unit");
            resourceRequest.setUsedUnit("0");
            resourceRequestList.add(resourceRequest);
        }
        return resourceRequestList;
    }


    public void buildUsageMonitoringUpdate(MongoDBConnect dbConnect) {
        try {
            PCEFInstance pcefInstance = appInstance.getPcefInstance();

            //find other transaction and filter
            List<Transaction> otherTransactionStartList = dbConnect.getTransactionService().findOtherStartTransaction(pcefInstance.getProfile().getUserValue());
            dbConnect.getQuotaService().filterTransactionConfirmIsNewResource(otherTransactionStartList);
            appInstance.getPcefInstance().getOtherStartTransactions().addAll(otherTransactionStartList);

            Operation operation = Operation.UsageMonitoringUpdate;
            String invokeId = "umUpdate_";

            OCFUsageMonitoringRequest ocfUsageMonitoringRequest = new OCFUsageMonitoringRequest();
            ocfUsageMonitoringRequest.setCommand("usageMonitoringUpdate");
            ocfUsageMonitoringRequest.setSessionId(pcefInstance.getProfile().getSessionId());

            List<Transaction> transactionStartList = new ArrayList<>();
            if (pcefInstance.getCommitDatas().size() > 0) {
                setActualTimeByRequestType(ocfUsageMonitoringRequest, appInstance.getRequestType());
                ocfUsageMonitoringRequest.setResources(getResourceCommit(pcefInstance.getCommitDatas(), appInstance.getRequestType())); //set unit to Resource
            } else {
                /**Update New Resource**/
                transactionStartList.add(pcefInstance.getTransaction());
                ocfUsageMonitoringRequest.setActualTime(appInstance.getPcefInstance().getTransaction().getActualTime());
            }

            buildUsageMonitoring(ocfUsageMonitoringRequest, operation, invokeId, transactionStartList);
            PCEFUtils.writeMessageFlow("Build Usage Monitoring Update Request", MessageFlow.Status.Success, appInstance.getPcefInstance().getSessionId());
        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Build Usage Monitoring Update Request", MessageFlow.Status.Error, appInstance.getPcefInstance().getSessionId());
        }
    }
    /*  public void buildUsageMonitoringUpdate(MongoDBConnect dbConnect) {
        try {
            //find other transaction and filter
            List<Transaction> otherTransactionStartList = dbConnect.getTransactionService().findOtherStartTransaction();
            dbConnect.getQuotaService().filterTransactionConfirmIsNewResource(otherTransactionStartList);
            appInstance.getPcefInstance().getOtherStartTransactions().addAll(otherTransactionStartList);


            PCEFInstance pcefInstance = appInstance.getPcefInstance();
            Operation operation = Operation.UsageMonitoringUpdate;
            String invokeId = "umUpdate_";

            OCFUsageMonitoringRequest ocfUsageMonitoringRequest = new OCFUsageMonitoringRequest();
            ocfUsageMonitoringRequest.setCommand("usageMonitoringUpdate");
            ocfUsageMonitoringRequest.setSessionId(pcefInstance.getProfile().getSessionId());


            List<Transaction> transactionStartList = new ArrayList<>();
            if (pcefInstance.doCommit()) {//update commit and reserve
                Quota quotaExhaust = pcefInstance.getCommitPart().getQuotaExhaust();
                List<Quota> quotExpireList = pcefInstance.getCommitPart().getQuotaExpireList();
                Map<String, Integer> countUnitMap = pcefInstance.getCommitPart().getCountUnitMap();

                *//**Update Quota Exhaust**//*
                if (quotaExhaust != null) {
                    ocfUsageMonitoringRequest.setResources(getResourceCommitExhaustRequest(quotaExhaust, countUnitMap)); //set unit to Resource
                    ocfUsageMonitoringRequest.setActualTime(appInstance.getPcefInstance().getTransaction().getActualTime());
                }
                *//**Update Quota Expire**//*
                else if (quotExpireList.size() > 0) {
                    ocfUsageMonitoringRequest.setResources(getResourceCommitExpireRequest(quotExpireList, countUnitMap));  //set unit to Resource
                    ocfUsageMonitoringRequest.setActualTime(PCEFUtils.actualTimeDFM.format(new Date()));
                }

            } else {
                */

    /**
     * Update New Resource
     **//*
                transactionStartList.add(pcefInstance.getTransaction());
                ocfUsageMonitoringRequest.setActualTime(appInstance.getPcefInstance().getTransaction().getActualTime());
            }

            buildUsageMonitoring(ocfUsageMonitoringRequest, operation, invokeId, transactionStartList);
            PCEFUtils.writeMessageFlow("Build Usage Monitoring Update Request", MessageFlow.Status.Success, appInstance.getPcefInstance().getSessionId());
        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Build Usage Monitoring Update Request", MessageFlow.Status.Error, appInstance.getPcefInstance().getSessionId());
        }
    }*/
    private void setActualTimeByRequestType(OCFUsageMonitoringRequest ocfUsageMonitoringRequest, ERequestType requestType) {
        if (ERequestType.USAGE_MONITORING.equals(requestType)) {
            ocfUsageMonitoringRequest.setActualTime(appInstance.getPcefInstance().getTransaction().getActualTime());
        } else if (ERequestType.E11_TIMEOUT.equals(requestType)) {
            ocfUsageMonitoringRequest.setActualTime(PCEFUtils.actualTimeDFM.format(new Date()));
        } else if (ERequestType.GyRAR.equals(requestType)) {

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


    public void buildUsageMonitoringStop() {
        try {
            PCEFInstance pcefInstance = appInstance.getPcefInstance();
            Operation operation = Operation.UsageMonitoringStop;
            String invokeId = "umStop_";

            OCFUsageMonitoringRequest ocfUsageMonitoringRequest = new OCFUsageMonitoringRequest();
            ocfUsageMonitoringRequest.setCommand("usageMonitoringStop");
            ocfUsageMonitoringRequest.setSessionId(pcefInstance.getProfile().getSessionId());

            ocfUsageMonitoringRequest.setResources(getResourceStopRequest(appInstance.getPcefInstance().getCommitDatas()));

            List<Transaction> transactionStartList = new ArrayList<>();

            ocfUsageMonitoringRequest.setActualTime(PCEFUtils.actualTimeDFM.format(new Date()));

            buildUsageMonitoring(ocfUsageMonitoringRequest, operation, invokeId, transactionStartList);
            PCEFUtils.writeMessageFlow("Build Usage Monitoring Stop Request", MessageFlow.Status.Success, appInstance.getPcefInstance().getSessionId());
        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Build Usage Monitoring Stop Request", MessageFlow.Status.Error, appInstance.getPcefInstance().getSessionId());
        }
    }


    public OCFUsageMonitoringResponse readUsageMonitoringStop() throws Exception {

        try {
            Operation operation = Operation.UsageMonitoringStop;

            //extract
            OCFUsageMonitoringResponse OCFUsageMonitoringResponse = (OCFUsageMonitoringResponse) extractResponse(operation);


            //validate
            ValidateMessage.validateTestData();

            //increase stat success
//            PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.PCEF_RECEIVE_TEST_DATA);

            //summarylog res
//            appInstance.setSummaryLogExternalResponse(Operation.TestOperation, SummaryLog.getSummaryLogResponse(Operation.TestOperation, testResponseData));

            PCEFUtils.writeMessageFlow("Read Usage Monitoring Stop Response", MessageFlow.Status.Success, appInstance.getPcefInstance().getSessionId());
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


            PCEFUtils.writeMessageFlow("Read Usage Monitoring Stop Response", MessageFlow.Status.Error, appInstance.getPcefInstance().getSessionId());
            throw e;
        }
    }

}
