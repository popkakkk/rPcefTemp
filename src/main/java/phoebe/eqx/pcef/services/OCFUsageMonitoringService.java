package phoebe.eqx.pcef.services;

import ec02.af.utils.AFLog;
import ec02.data.interfaces.EquinoxRawData;
import phoebe.eqx.pcef.core.data.ResourceQuota;
import phoebe.eqx.pcef.core.data.ResourceResponse;
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


    public boolean receiveQuotaAndPolicy(OCFUsageMonitoringResponse ocfUsageMonitoringResponse) {
        boolean checkReceiveQuotaAndPolicy = true;

        List<String> resourceIdResponseList = new ArrayList<>();
        ocfUsageMonitoringResponse.getResources().forEach(resourceResponse -> resourceIdResponseList.add(resourceResponse.getResourceId()));

        //Check New Resource
        List<ResourceRequest> newResourceRequestList = context.getPcefInstance().getNewResources();

        for (ResourceRequest newResourceRequest : newResourceRequestList) {
            if (!resourceIdResponseList.contains(newResourceRequest.getResourceId())) {
                checkReceiveQuotaAndPolicy = false;
                break;
            }
        }

        //Check Commit Resource
        List<CommitData> commitDataList = context.getPcefInstance().getCommitDatas();
        for (CommitData commitData : commitDataList) {
            if (commitData.getCount() == 0) {
                continue;
            } else {
                String resourceId = commitData.get_id().getResourceId();
                if (!resourceIdResponseList.contains(resourceId)) {
                    checkReceiveQuotaAndPolicy = false;
                    break;
                }
            }
        }

        return checkReceiveQuotaAndPolicy;
    }


    private String generateSessionId() {
        return "sessionIdFromPCEF";
    }

    private String generateTransactionId() {
        return "transactionId" + context.getPcefInstance().getProfile().getSequenceNumber();
    }


    public void buildUsageMonitoringStart(MongoDBConnect dbConnect) {
        try {
            AFLog.d("Build Usage Monitoring Start Request..");

            //find other transaction
            List<Transaction> otherTransactionStartList = dbConnect.getTransactionService().findOtherStartTransaction(context.getPcefInstance().getProfile().getUserValue());
            context.getPcefInstance().getOtherStartTransactions().addAll(otherTransactionStartList);

            Operation operation = Operation.UsageMonitoringStart;
            String invokeId = generateInvokeId(operation);

            OCFUsageMonitoringRequest umStartRequest = new OCFUsageMonitoringRequest();
            umStartRequest.setCommand("usageMonitoringStart");

            String sessionId = generateSessionId();
            context.getPcefInstance().getProfile().setSessionId(sessionId);
            umStartRequest.setSessionId(sessionId);//generate
            umStartRequest.setActualTime(context.getPcefInstance().getTransaction().getActualTime());

            List<Transaction> transactionList = new ArrayList<>();
            transactionList.add(context.getPcefInstance().getTransaction());

            buildUsageMonitoring(umStartRequest, operation, invokeId, transactionList);

            PCEFUtils.writeMessageFlow("Build Usage Monitoring Start Request", MessageFlow.Status.Success, context.getPcefInstance().getSessionId());
        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Build Usage Monitoring Start Request" + e.getStackTrace()[0], MessageFlow.Status.Error, context.getPcefInstance().getSessionId());
        }
    }


    private void buildUsageMonitoring(OCFUsageMonitoringRequest umRequest, Operation operation, String invokeId, List<Transaction> transactionList) throws Exception {

        PCEFInstance pcefInstance = context.getPcefInstance();


        umRequest.setUserType(pcefInstance.getProfile().getUserType());
        umRequest.setUserValue(pcefInstance.getProfile().getUserValue());

        pcefInstance.getProfile().increaseSequenceNumber();
        umRequest.setTid(generateTransactionId());
        umRequest.setRequestNumber(String.valueOf(pcefInstance.getProfile().getSequenceNumber()));

        transactionList.addAll(context.getPcefInstance().getOtherStartTransactions());

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
                context.getPcefInstance().getNewResources().add(resourceRequest);

                //for check duplicate resourceId
                resourceIdList.add(resourceId);
            }
        }


        MessagePool messagePool = new MessagePool(abstractAF);
        EquinoxRawData equinoxRawData = messagePool.getOCFUsageMonitoringRequest(umRequest, invokeId);
        invokeExternal(equinoxRawData, operation, messagePool.getRequestObj());
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
                resourceRequest.setRtid(context.getPcefInstance().getTransaction().getRtid());
                resourceRequest.setReportingReason("0");
            } else if (ERequestType.E11_TIMEOUT.equals(requestType)) {
//                resourceRequest.setRtid(context.getPcefInstance().getTransaction().getRtid()); // last of tid

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
            AFLog.d("Build Usage Monitoring Update Request ..");

            PCEFInstance pcefInstance = context.getPcefInstance();

            //find other transaction and filter
            List<Transaction> otherTransactionStartList = dbConnect.getTransactionService().findOtherStartTransaction(pcefInstance.getProfile().getUserValue());
            dbConnect.getQuotaService().filterTransactionConfirmIsNewResource(otherTransactionStartList);
            context.getPcefInstance().getOtherStartTransactions().addAll(otherTransactionStartList);

            Operation operation = Operation.UsageMonitoringUpdate;
            String invokeId = generateInvokeId(operation);

            OCFUsageMonitoringRequest ocfUsageMonitoringRequest = new OCFUsageMonitoringRequest();
            ocfUsageMonitoringRequest.setCommand("usageMonitoringUpdate");
            ocfUsageMonitoringRequest.setSessionId(pcefInstance.getProfile().getSessionId());

            List<Transaction> transactionStartList = new ArrayList<>();
            if (pcefInstance.getCommitDatas().size() > 0) {
                setActualTimeByRequestType(ocfUsageMonitoringRequest, context.getRequestType());
                ocfUsageMonitoringRequest.setResources(getResourceCommit(pcefInstance.getCommitDatas(), context.getRequestType())); //set unit to Resource
            } else {
                /**Update New Resource**/
                transactionStartList.add(pcefInstance.getTransaction());
                ocfUsageMonitoringRequest.setActualTime(context.getPcefInstance().getTransaction().getActualTime());
            }

            buildUsageMonitoring(ocfUsageMonitoringRequest, operation, invokeId, transactionStartList);
            PCEFUtils.writeMessageFlow("Build Usage Monitoring Update Request", MessageFlow.Status.Success, context.getPcefInstance().getSessionId());
        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Build Usage Monitoring Update Request", MessageFlow.Status.Error, context.getPcefInstance().getSessionId());
        }
    }

    private void setActualTimeByRequestType(OCFUsageMonitoringRequest ocfUsageMonitoringRequest, ERequestType requestType) {
        if (ERequestType.USAGE_MONITORING.equals(requestType)) {
            ocfUsageMonitoringRequest.setActualTime(context.getPcefInstance().getTransaction().getActualTime());
        } else if (ERequestType.E11_TIMEOUT.equals(requestType)) {
            ocfUsageMonitoringRequest.setActualTime(PCEFUtils.actualTimeDFM.format(new Date()));
        } else if (ERequestType.GyRAR.equals(requestType)) {

        }
    }

    public OCFUsageMonitoringResponse readUsageMonitoringStart() throws Exception {

        try {
            AFLog.d("Read Usage Monitoring Start Response ..");

            Operation operation = Operation.UsageMonitoringStart;

            //extract
            OCFUsageMonitoringResponse OCFUsageMonitoringResponse = (OCFUsageMonitoringResponse) extractResponse(operation);
//            this.context.getPcefInstance().setUsageMonitoring(OCFUsageMonitoringResponse);

            //validate
            ValidateMessage.validateTestData();

            //increase stat success
//            PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.PCEF_RECEIVE_TEST_DATA);

            //summarylog res
//            context.setSummaryLogExternalResponse(Operation.TestOperation, SummaryLog.getSummaryLogResponse(Operation.TestOperation, testResponseData));

            PCEFUtils.writeMessageFlow("Read Usage Monitoring Start Response", MessageFlow.Status.Success, context.getPcefInstance().getSessionId());
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


            PCEFUtils.writeMessageFlow("Read Usage Monitoring Start Response", MessageFlow.Status.Error, context.getPcefInstance().getSessionId());
            throw e;
        }

    }

    public OCFUsageMonitoringResponse readUsageMonitoringUpdate() throws Exception {

        try {
            AFLog.d("Read Usage Monitoring Update Response ..");

            Operation operation = Operation.UsageMonitoringUpdate;

            //extract
            OCFUsageMonitoringResponse OCFUsageMonitoringResponse = (OCFUsageMonitoringResponse) extractResponse(operation);
//            this.context.getPcefInstance().setUsageMonitoring(OCFUsageMonitoringResponse);

            //validate
            ValidateMessage.validateTestData();

            //increase stat success
//            PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.PCEF_RECEIVE_TEST_DATA);

            //summarylog res
//            context.setSummaryLogExternalResponse(Operation.TestOperation, SummaryLog.getSummaryLogResponse(Operation.TestOperation, testResponseData));

            PCEFUtils.writeMessageFlow("Read Usage Monitoring Update Response", MessageFlow.Status.Success, context.getPcefInstance().getSessionId());
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


            PCEFUtils.writeMessageFlow("Read Usage Monitoring Start Response", MessageFlow.Status.Error, context.getPcefInstance().getSessionId());
            throw e;
        }
    }


    public void buildUsageMonitoringStop() {
        try {
            AFLog.d("Build Usage Monitoring Stop Request..");

            PCEFInstance pcefInstance = context.getPcefInstance();
            Operation operation = Operation.UsageMonitoringStop;
            String invokeId = generateInvokeId(operation);

            OCFUsageMonitoringRequest ocfUsageMonitoringRequest = new OCFUsageMonitoringRequest();
            ocfUsageMonitoringRequest.setCommand("usageMonitoringStop");
            ocfUsageMonitoringRequest.setSessionId(pcefInstance.getProfile().getSessionId());

            ocfUsageMonitoringRequest.setResources(getResourceStopRequest(context.getPcefInstance().getCommitDatas()));

            List<Transaction> transactionStartList = new ArrayList<>();

            ocfUsageMonitoringRequest.setActualTime(PCEFUtils.actualTimeDFM.format(new Date()));

            buildUsageMonitoring(ocfUsageMonitoringRequest, operation, invokeId, transactionStartList);
            PCEFUtils.writeMessageFlow("Build Usage Monitoring Stop Request", MessageFlow.Status.Success, context.getPcefInstance().getSessionId());
        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Build Usage Monitoring Stop Request", MessageFlow.Status.Error, context.getPcefInstance().getSessionId());
        }
    }


    public OCFUsageMonitoringResponse readUsageMonitoringStop() throws Exception {

        try {
            AFLog.d("Read Usage Monitoring Stop Response..");
            Operation operation = Operation.UsageMonitoringStop;

            //extract
            OCFUsageMonitoringResponse OCFUsageMonitoringResponse = (OCFUsageMonitoringResponse) extractResponse(operation);


            //validate
            ValidateMessage.validateTestData();

            //increase stat success
//            PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.PCEF_RECEIVE_TEST_DATA);

            //summarylog res
//            context.setSummaryLogExternalResponse(Operation.TestOperation, SummaryLog.getSummaryLogResponse(Operation.TestOperation, testResponseData));

            PCEFUtils.writeMessageFlow("Read Usage Monitoring Stop Response", MessageFlow.Status.Success, context.getPcefInstance().getSessionId());
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


            PCEFUtils.writeMessageFlow("Read Usage Monitoring Stop Response", MessageFlow.Status.Error, context.getPcefInstance().getSessionId());
            throw e;
        }
    }

}
