package phoebe.eqx.pcef.services;

import ec02.af.utils.AFLog;
import ec02.data.interfaces.EquinoxRawData;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import phoebe.eqx.pcef.core.data.ResourceQuota;
import phoebe.eqx.pcef.core.data.ResourceRequest;
import phoebe.eqx.pcef.core.data.ResourceResponse;
import phoebe.eqx.pcef.core.exceptions.PCEFException;
import phoebe.eqx.pcef.core.exceptions.ResponseErrorException;
import phoebe.eqx.pcef.core.exceptions.TimeoutException;
import phoebe.eqx.pcef.core.model.Quota;
import phoebe.eqx.pcef.core.model.Transaction;
import phoebe.eqx.pcef.enums.EError;
import phoebe.eqx.pcef.enums.ERequestType;
import phoebe.eqx.pcef.enums.Operation;
import phoebe.eqx.pcef.enums.stats.EStatCmd;
import phoebe.eqx.pcef.enums.stats.EStatMode;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.instance.CommitData;
import phoebe.eqx.pcef.instance.PCEFInstance;
import phoebe.eqx.pcef.message.builder.MessagePool;
import phoebe.eqx.pcef.message.builder.req.OCFUsageMonitoringRequest;
import phoebe.eqx.pcef.message.parser.res.OCFUsageMonitoringResponse;
import phoebe.eqx.pcef.services.mogodb.MongoDBConnect;
import phoebe.eqx.pcef.utils.MessageFlow;
import phoebe.eqx.pcef.utils.PCEFUtils;
import phoebe.eqx.pcef.utils.ValidateMessage;

import java.util.*;

public class OCFUsageMonitoringService extends PCEFService {
    public OCFUsageMonitoringService(AppInstance appInstance) {
        super(appInstance);
    }

    public static final String CMD_UM_START = "usageMonitoringStart";
    public static final String CMD_UM_UPDATE = "usageMonitoringUpdate";
    public static final String CMD_UM_STOP = "usageMonitoringStop";

    private String generateSessionId() {
        return "PCEF:ACTIVE:" + PCEFUtils.actualTimeDFM.format(appInstance.getMyContext().getStartTime());
    }

    private String generateTransactionId() {
        String sequenceNumber = String.valueOf(context.getPcefInstance().getProfile().getSequenceNumber());
        return "PCEF" + StringUtils.leftPad(sequenceNumber, 10, "0");
    }


    private void buildUsageMonitoring(OCFUsageMonitoringRequest umRequest, Operation operation, String invokeId, List<Transaction> transactionList) {

        PCEFInstance pcefInstance = context.getPcefInstance();

        umRequest.setUserType(pcefInstance.getProfile().getUserType());
        umRequest.setUserValue(pcefInstance.getProfile().getUserValue());

        pcefInstance.getProfile().increaseSequenceNumber();
        umRequest.setTid(generateTransactionId());
        umRequest.setRequestNumber(String.valueOf(pcefInstance.getProfile().getSequenceNumber()));

        //add Other start
        transactionList.addAll(context.getPcefInstance().getOtherStartTransactions());

        //set all new resource
//        List<String> resourceIdList = new ArrayList<>();
        ArrayList<ResourceRequest> newResources = new ArrayList<>();

        HashMap<String, Transaction> transactionHashMap = new HashMap<>();
        for (Transaction t : transactionList) {
            Transaction lastTransactionResourceId = transactionHashMap.get(t.getResourceId());
            if (lastTransactionResourceId == null) {
                transactionHashMap.put(t.getResourceId(), t);
            } else {
                if (t.getCreateDate().after(lastTransactionResourceId.getCreateDate())) {
                    transactionHashMap.replace(t.getResourceId(), t);
                }
            }
        }

        transactionHashMap.forEach((s, transaction) -> {
            ResourceRequest resourceRequest = new ResourceRequest();
            resourceRequest.setResourceId(transaction.getResourceId());
            resourceRequest.setResourceName(transaction.getResourceName());
            resourceRequest.setRtid(transaction.getRtid());
            newResources.add(resourceRequest);
        });
        umRequest.getResources().addAll(newResources);

        /*for (Transaction t : transactionList) {
            String resourceId = t.getResourceId();

            Transaction tRequest = transactionHashMap.get(resourceId);

            if (tRequest == null) {
                ResourceRequest r = new ResourceRequest();
                r.setResourceId(resourceId);
                r.setResourceName(t.getResourceName());
                r.setRtid(t.getRtid());

                //add resources
                newResources.add(r);
//                context.getPcefInstance().getNewResourcesRequests().add(resourceRequest);

                //for check duplicate resourceId
                transactionHashMap.put(resourceId, t);
//                resourceIdList.add(resourceId);
            } else {
                if (t.getCreateDate().after(tRequest.getCreateDate())) {
                    tRequest.
                }

            }
        }*/



        MessagePool messagePool = new MessagePool(abstractAF);
        EquinoxRawData equinoxRawData = messagePool.getOCFUsageMonitoringRequest(umRequest, invokeId);
        invokeExternal(equinoxRawData, operation, messagePool.getRequestObj());
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
            umStartRequest.setCommand(CMD_UM_START);

            String sessionId = generateSessionId();
            context.getPcefInstance().getProfile().setSessionId(sessionId);
            umStartRequest.setSessionId(sessionId);//generate
            umStartRequest.setActualTime(context.getPcefInstance().getTransaction().getActualTime());

            List<Transaction> transactionList = new ArrayList<>();
            transactionList.add(context.getPcefInstance().getTransaction());

            buildUsageMonitoring(umStartRequest, operation, invokeId, transactionList);

            PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.sent_Usage_Monitoring_Start_request);
        } catch (Exception e) {
            PCEFException pcefException = new PCEFException();
            pcefException.setError(EError.USAGE_MONITORING_START_BUILD_REQUEST_ERROR);
            pcefException.setErrorMsg(ExceptionUtils.getStackTrace(e));

            context.setPcefException(pcefException);
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.ERROR, EStatCmd.sent_Usage_Monitoring_Start_request);
            throw e;
        }
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
            ocfUsageMonitoringRequest.setCommand(CMD_UM_UPDATE);
            ocfUsageMonitoringRequest.setSessionId(pcefInstance.getProfile().getSessionId());


            List<Transaction> transactionList = new ArrayList<>();
            if (pcefInstance.getCommitDatas().size() > 0) {
                /**Update Commit Resource**/
                ocfUsageMonitoringRequest.setResources(getResourceCommit(pcefInstance.getCommitDatas(), context.getRequestType())); //set unit to Resource
                setActualTimeByRequestType(ocfUsageMonitoringRequest, context.getRequestType());

                if (pcefInstance.isSameMkExhaust()) {
                    transactionList.add(pcefInstance.getTransaction());
                    pcefInstance.setSameMkExhaust(false);
                }

            } else {
                /**Update New Resource**/
                transactionList.add(pcefInstance.getTransaction());
                ocfUsageMonitoringRequest.setActualTime(context.getPcefInstance().getTransaction().getActualTime());
            }

            buildUsageMonitoring(ocfUsageMonitoringRequest, operation, invokeId, transactionList);

            PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.sent_Usage_Monitoring_Update_request);
        } catch (Exception e) {
            PCEFException pcefException = new PCEFException();
            pcefException.setError(EError.USAGE_MONITORING_UPDATE_BUILD_REQUEST_ERROR);
            pcefException.setErrorMsg(ExceptionUtils.getStackTrace(e));

            context.setPcefException(pcefException);
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.ERROR, EStatCmd.sent_Usage_Monitoring_Update_request);
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
            ocfUsageMonitoringRequest.setCommand(CMD_UM_STOP);
            ocfUsageMonitoringRequest.setSessionId(pcefInstance.getProfile().getSessionId());

            ocfUsageMonitoringRequest.setResources(getResourceStopRequest(context.getPcefInstance().getCommitDatas()));

            List<Transaction> transactionStartList = new ArrayList<>();

            ocfUsageMonitoringRequest.setActualTime(PCEFUtils.actualTimeDFM.format(new Date()));

            buildUsageMonitoring(ocfUsageMonitoringRequest, operation, invokeId, transactionStartList);
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.sent_Usage_Monitoring_Stop_request);
        } catch (Exception e) {
            PCEFException pcefException = new PCEFException();
            pcefException.setError(EError.USAGE_MONITORING_STOP_BUILD_REQUEST_ERROR);
            pcefException.setErrorMsg(ExceptionUtils.getStackTrace(e));

            context.setPcefException(pcefException);

            PCEFUtils.increaseStatistic(abstractAF, EStatMode.ERROR, EStatCmd.sent_Usage_Monitoring_Stop_request);
            throw e;
        }
    }

    public OCFUsageMonitoringResponse readUsageMonitoringStart() throws Exception {
        try {
            try {
                AFLog.d("Read Usage Monitoring Start Response ..");

                Operation operation = Operation.UsageMonitoringStart;
                OCFUsageMonitoringResponse ocfUsageMonitoringResponse;

                ocfUsageMonitoringResponse = (OCFUsageMonitoringResponse) extractResponse(operation);

                ValidateMessage.validateUsageMonitoringStart(ocfUsageMonitoringResponse, abstractAF);


                //summarylog res
//            context.setSummaryLogExternalResponse(Operation.TestOperation, SummaryLog.getSummaryLogResponse(Operation.TestOperation, testResponseData));


                PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.receive_Usage_Monitoring_Start_response);
                return ocfUsageMonitoringResponse;
            } catch (TimeoutException e) {
                PCEFUtils.increaseStatistic(abstractAF, EStatMode.TIMEOUT, EStatCmd.receive_Usage_Monitoring_Start_response);
                e.setError(EError.USAGE_MONITORING_START_RESPONSE_TIMEOUT);
                throw e;
            } catch (ResponseErrorException e) {
                PCEFUtils.increaseStatistic(abstractAF, EStatMode.EQUINOX_ERROR, EStatCmd.receive_Usage_Monitoring_Start_response);
                e.setError(EError.USAGE_MONITORING_START_RESPONSE_EQUINOX_ERROR);
                throw e;
            }
        } catch (PCEFException e) {
            context.setPcefException(e);
            throw e;
        }

    }

    public OCFUsageMonitoringResponse readUsageMonitoringUpdate() throws Exception {

        try {
            try {
                AFLog.d("Read Usage Monitoring Update Response ..");

                Operation operation = Operation.UsageMonitoringUpdate;
                OCFUsageMonitoringResponse ocfUsageMonitoringResponse = (OCFUsageMonitoringResponse) extractResponse(operation);

                ValidateMessage.validateUsageMonitoringUpdate(ocfUsageMonitoringResponse, abstractAF);

                //summarylog res
//            context.setSummaryLogExternalResponse(Operation.TestOperation, SummaryLog.getSummaryLogResponse(Operation.TestOperation, testResponseData));


                PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.receive_Usage_Monitoring_Update_response);
                return ocfUsageMonitoringResponse;
            } catch (TimeoutException e) {
                PCEFUtils.increaseStatistic(abstractAF, EStatMode.TIMEOUT, EStatCmd.receive_Usage_Monitoring_Update_response);
                e.setError(EError.USAGE_MONITORING_UPDATE_RESPONSE_TIMEOUT);
                throw e;
            } catch (ResponseErrorException e) {
                PCEFUtils.increaseStatistic(abstractAF, EStatMode.EQUINOX_ERROR, EStatCmd.receive_Usage_Monitoring_Update_response);
                e.setError(EError.USAGE_MONITORING_UPDATE_RESPONSE_EQUINOX_ERROR);
                throw e;
            }
        } catch (PCEFException e) {
            //summarylog fail
            context.setPcefException(e);
            throw e;
        }

    }


    public OCFUsageMonitoringResponse readUsageMonitoringStop() throws Exception {

        try {
            try {
                AFLog.d("Read Usage Monitoring Stop Response..");
                Operation operation = Operation.UsageMonitoringStop;
                OCFUsageMonitoringResponse ocfUsageMonitoringResponse = (OCFUsageMonitoringResponse) extractResponse(operation);

                ValidateMessage.validateUsageMonitoringStop(ocfUsageMonitoringResponse, abstractAF);

                //summarylog res
//            context.setSummaryLogExternalResponse(Operation.TestOperation, SummaryLog.getSummaryLogResponse(Operation.TestOperation, testResponseData));


                PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.receive_Usage_Monitoring_Stop_response);
                return ocfUsageMonitoringResponse;
            } catch (TimeoutException e) {
                PCEFUtils.increaseStatistic(abstractAF, EStatMode.TIMEOUT, EStatCmd.receive_Usage_Monitoring_Stop_response);
                e.setError(EError.USAGE_MONITORING_STOP_RESPONSE_TIMEOUT);
                throw e;
            } catch (ResponseErrorException e) {
                PCEFUtils.increaseStatistic(abstractAF, EStatMode.EQUINOX_ERROR, EStatCmd.receive_Usage_Monitoring_Stop_response);
                e.setError(EError.USAGE_MONITORING_STOP_RESPONSE_EQUINOX_ERROR);
                throw e;
            }
        } catch (PCEFException e) {

            context.setPcefException(e);
            throw e;
        }
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
                if (commitData.get_id().getResourceId().equals(context.getPcefInstance().getTransaction().getResourceId())) {
                    resourceRequest.setRtid(context.getPcefInstance().getTransaction().getRtid());
                } else {
                    resourceRequest.setRtid(commitData.getLastRtid());
                }
                resourceRequest.setReportingReason("0");
            } else if (ERequestType.E11_TIMEOUT.equals(requestType)) {
                resourceRequest.setRtid(commitData.getLastRtid()); // last tid of profile

                if (unit == 0) {
                    resourceRequest.setReportingReason("1");//sent terminate by resource
                } else {
                    resourceRequest.setReportingReason("0");
                }

            } else if (ERequestType.GyRAR.equals(requestType)) {
                resourceRequest.setRtid(commitData.getLastRtid());// last tid of profile
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
            resourceRequest.setReportingReason("0");
            resourceRequestList.add(resourceRequest);
        }
        return resourceRequestList;
    }


    private void setActualTimeByRequestType(OCFUsageMonitoringRequest ocfUsageMonitoringRequest, ERequestType requestType) {
        if (ERequestType.USAGE_MONITORING.equals(requestType)) {
            ocfUsageMonitoringRequest.setActualTime(context.getPcefInstance().getTransaction().getActualTime());
        } else if (ERequestType.E11_TIMEOUT.equals(requestType)) {
            ocfUsageMonitoringRequest.setActualTime(PCEFUtils.actualTimeDFM.format(new Date()));
        } else if (ERequestType.GyRAR.equals(requestType)) {

        }
    }

    public ArrayList<Quota> getQuotaFromUsageMonitoringResponse(OCFUsageMonitoringResponse OCFUsageMonitoringResponse) {
        Map<String, Quota> quotaMap = new HashMap<>();
        for (ResourceResponse resourceResponse : OCFUsageMonitoringResponse.getResources()) {

            if (resourceResponse.getResultDesc().toLowerCase().contains("error") && !resourceResponse.getResultDesc().toLowerCase().contains("commit_error")) {
                continue;
            }

            String monitoringKey = resourceResponse.getMonitoringKey();
            String resourceName = resourceResponse.getResourceName();
            String resourceId = resourceResponse.getResourceId();

            ResourceQuota resourceQuota = new ResourceQuota();
            resourceQuota.setResourceId(resourceId);
            resourceQuota.setResourceName(resourceName);

            Quota myQuota = quotaMap.get(monitoringKey);
            if (myQuota == null) {
                Quota quota = new Quota();
                quota.set_id(resourceResponse.getMonitoringKey());
                quota.setUserType(OCFUsageMonitoringResponse.getUserType());
                quota.setUserValue(OCFUsageMonitoringResponse.getUserValue());
                quota.setProcessing(0);
                quota.setMonitoringKey(resourceResponse.getMonitoringKey());
                quota.setCounterId(resourceResponse.getCounterId());

                if (resourceResponse.getQuotaByKey() != null) {
                    quota.setQuotaByKey(resourceResponse.getQuotaByKey());
                    quota.setRateLimitByKey(resourceResponse.getRateLimitByKey());

                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.SECOND, resourceResponse.getQuotaByKey().getValidityTime());
                    quota.setExpireDate(calendar.getTime());
                }
                quota.getResources().add(resourceQuota);
                quotaMap.put(monitoringKey, quota);
            } else {
                //Same Quota --> update resourceResponse
                myQuota.getResources().add(resourceQuota);
            }
        }
        return new ArrayList<>(quotaMap.values());
    }


}
