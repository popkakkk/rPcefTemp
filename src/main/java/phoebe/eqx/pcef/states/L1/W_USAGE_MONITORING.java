package phoebe.eqx.pcef.states.L1;

import ec02.af.utils.AFLog;
import phoebe.eqx.pcef.core.model.Quota;
import phoebe.eqx.pcef.core.model.Transaction;
import phoebe.eqx.pcef.enums.state.EState;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.instance.CommitData;
import phoebe.eqx.pcef.message.parser.res.OCFUsageMonitoringResponse;
import phoebe.eqx.pcef.services.GenerateCDRService;
import phoebe.eqx.pcef.services.GetResourceIdService;
import phoebe.eqx.pcef.services.OCFUsageMonitoringService;
import phoebe.eqx.pcef.services.UsageMonitoringService;
import phoebe.eqx.pcef.services.mogodb.MongoDBConnect;
import phoebe.eqx.pcef.states.abs.ComplexState;
import phoebe.eqx.pcef.states.abs.MessageRecieved;
import phoebe.eqx.pcef.states.L2.W_MONGODB_PROCESS_USAGE_MONITORING;

import java.util.ArrayList;
import java.util.List;

public class W_USAGE_MONITORING extends ComplexState {

    public W_USAGE_MONITORING(AppInstance appInstance) {
        super(appInstance, Level.L1);
        this.setState(EState.BEGIN);
    }

    @MessageRecieved(messageType = EState.BEGIN)
    public void begin() throws Exception {
        UsageMonitoringService usageMonitoringService = new UsageMonitoringService(appInstance);
        usageMonitoringService.readUsageMonitoringRequest();

        GetResourceIdService getResourceIdService = new GetResourceIdService(appInstance);
        getResourceIdService.buildGetResourceId();
        setWorkState(EState.W_GET_RESOURCE_ID);
    }

    @MessageRecieved(messageType = EState.W_GET_RESOURCE_ID)
    public void wGetResourceId() throws Exception {
        GetResourceIdService getResourceIdService = new GetResourceIdService(appInstance);
        getResourceIdService.readGetResourceId();
        setWorkState(EState.W_MONGO_PROCESS_USAGE_MONITORING);
    }


    @MessageRecieved(messageType = EState.W_MONGO_PROCESS_USAGE_MONITORING)
    public void wMongoUsageMonitoring() throws Exception {
        MongoDBConnect dbConnect = null;
        EState nextState = null;

        try {
            dbConnect = new MongoDBConnect(appInstance);

            //Application Logic wait mongodb process
            W_MONGODB_PROCESS_USAGE_MONITORING mongodbProcessState = new W_MONGODB_PROCESS_USAGE_MONITORING(appInstance, dbConnect);
            mongodbProcessState.dispatch();

            nextState = context.getStateL2();
            if (EState.END.equals(context.getStateL3())) {
                if (EState.END.equals(nextState)) {
                    UsageMonitoringService usageMonitoringService = new UsageMonitoringService(appInstance);
                    if (mongodbProcessState.isResponseSuccess()) {
                        usageMonitoringService.buildResponseUsageMonitoring(true);
                    } else {
                        usageMonitoringService.buildResponseUsageMonitoring(false);
                    }
                } else {
                    OCFUsageMonitoringService ocfUsageMonitoringService = new OCFUsageMonitoringService(appInstance);
                    if (EState.W_USAGE_MONITORING_START.equals(nextState)) {
                        AFLog.d("State is Usage Monitoring First Usage");
                        ocfUsageMonitoringService.buildUsageMonitoringStart(dbConnect);
                    } else if (EState.W_USAGE_MONITORING_UPDATE.equals(nextState)) {
                        ocfUsageMonitoringService.buildUsageMonitoringUpdate(dbConnect);
                    }
                }
            }
        } catch (Exception e) {
            if (dbConnect != null) {
                dbConnect.getTransactionService().deleteTransactionError();
            }

            AFLog.d("Mongodb initial process error:" + e.getStackTrace()[0]);
            throw e;
        } finally {
            if (dbConnect != null) {
                dbConnect.closeConnection();
            }
            if (nextState == null) {
                nextState = EState.END;
            }
        }

        setWorkState(nextState);
    }


    @MessageRecieved(messageType = EState.W_USAGE_MONITORING_START)
    public void wUsageMonitoringStart() throws Exception {
        OCFUsageMonitoringService ocfUsageMonitoringService = new OCFUsageMonitoringService(appInstance);
        OCFUsageMonitoringResponse ocfUsageMonitoringResponse = null;

        MongoDBConnect dbConnect = null;
        try {

            dbConnect = new MongoDBConnect(appInstance);
            ocfUsageMonitoringResponse = ocfUsageMonitoringService.readUsageMonitoringStart();


            UsageMonitoringService usageMonitoringService = new UsageMonitoringService(appInstance);

            List<Transaction> transactionList = new ArrayList<>();
            transactionList.add(appInstance.getMyContext().getPcefInstance().getTransaction());
            transactionList.addAll(appInstance.getMyContext().getPcefInstance().getOtherStartTransactions());

            dbConnect.getTransactionService().filterResourceRequestErrorNewResource(ocfUsageMonitoringResponse, appInstance.getMyContext().getPcefInstance().getNewResourcesRequests(), transactionList);
            ArrayList<Quota> quotaResponseList = dbConnect.getQuotaService().getQuotaFromUsageMonitoringResponse(ocfUsageMonitoringResponse);

            dbConnect.getQuotaService().insertQuotaInitial(quotaResponseList);
            dbConnect.getTransactionService().updateTransaction(quotaResponseList, transactionList);

            GenerateCDRService generateCDRService = new GenerateCDRService();
            generateCDRService.buildCDRCharging(transactionList, appInstance.getAbstractAF());

            dbConnect.getProfileService().updateProfileUnLockInitial(dbConnect.getQuotaService().getMinExpireDate());

            usageMonitoringService.buildResponseUsageMonitoring(true);
        } catch (Exception e) {
            AFLog.d("wUsageMonitoringStart error:" + e.getStackTrace()[0]);
            if (dbConnect != null) {
                dbConnect.getTransactionService().deleteTransactionError();
            }
            throw e;
        } finally {
            if (dbConnect != null) {
                dbConnect.closeConnection();
            }

        }
        setWorkState(EState.END);
    }


    @MessageRecieved(messageType = EState.W_USAGE_MONITORING_UPDATE)
    public void wUsageMonitoringUpdate() throws Exception {
        OCFUsageMonitoringService ocfUsageMonitoringService = new OCFUsageMonitoringService(appInstance);
        OCFUsageMonitoringResponse ocfUsageMonitoringResponse = ocfUsageMonitoringService.readUsageMonitoringUpdate();

        MongoDBConnect dbConnect = null;
        try {
            dbConnect = new MongoDBConnect(appInstance);
            UsageMonitoringService usageMonitoringService = new UsageMonitoringService(appInstance);


            List<Transaction> newResourceTransactions = new ArrayList<>();

            Transaction thisTransaction = appInstance.getMyContext().getPcefInstance().getTransaction();
            List<CommitData> commitDataList = appInstance.getMyContext().getPcefInstance().getCommitDatas();
            if (appInstance.getMyContext().getPcefInstance().getCommitDatas().size() == 0) {
                newResourceTransactions.add(thisTransaction); //this transaction new resource
            } else {
                for (CommitData commitData : commitDataList) {
                    if (commitData.get_id().getResourceId().equals(thisTransaction.getResourceId())) {
                        commitData.getTransactionIds().add(thisTransaction.getTid());//this transaction commit resource -- to filter error
                        break;
                    }
                }
            }


            newResourceTransactions.addAll(appInstance.getMyContext().getPcefInstance().getOtherStartTransactions());

            dbConnect.getTransactionService().filterResourceRequestErrorNewResource(ocfUsageMonitoringResponse, appInstance.getMyContext().getPcefInstance().getNewResourcesRequests(), newResourceTransactions);
            dbConnect.getTransactionService().filterResourceRequestErrorCommitResource(ocfUsageMonitoringResponse, appInstance.getMyContext().getPcefInstance().getCommitDatas());


            boolean transactionError = true;
            if (commitDataList.size() > 0) {  //check my transaction commit error and set to update
                for (CommitData commitData : commitDataList) {
                    if (thisTransaction.getResourceId().equals(commitData.get_id().getResourceId())) {
                        commitData.getTransactionIds().remove(commitDataList.size() - 1);
                        transactionError = false;
                        break;
                    }
                }
            } else {
                for (Transaction t : newResourceTransactions) {
                    if (thisTransaction.getResourceId().equals(t.getResourceId())) {
                        transactionError = false;
                        break;
                    }
                }

            }

            ArrayList<Quota> quotaResponseList = dbConnect.getQuotaService().getQuotaFromUsageMonitoringResponse(ocfUsageMonitoringResponse);

            dbConnect.getQuotaService().updateQuota(quotaResponseList);
            dbConnect.getTransactionService().updateTransaction(quotaResponseList, newResourceTransactions);

            GenerateCDRService generateCDRService = new GenerateCDRService();
            generateCDRService.buildCDRCharging(newResourceTransactions, appInstance.getAbstractAF());

            dbConnect.getProfileService().updateProfileUnLock(dbConnect.getQuotaService().isHaveNewQuota(), dbConnect.getQuotaService().getMinExpireDate());

            if (!transactionError) {
                dbConnect.getTransactionService().updateTransactionWaitingToComplete(quotaResponseList);
                usageMonitoringService.buildResponseUsageMonitoring(true);
            } else {
                dbConnect.getTransactionService().deleteTransactionError();
                usageMonitoringService.buildResponseUsageMonitoring(false);
            }

        } catch (Exception e) {
            AFLog.d("wUsageMonitoringUpdate error -" + e.getStackTrace()[0]);

            if (dbConnect != null) {
                dbConnect.getTransactionService().deleteTransactionError();
            }
            throw e;
        } finally {
            if (dbConnect != null) {
                dbConnect.closeConnection();
            }

        }
        setWorkState(EState.END);
    }


}
