package phoebe.eqx.pcef.states.L1;

import ec02.af.utils.AFLog;
import phoebe.eqx.pcef.core.model.Quota;
import phoebe.eqx.pcef.core.model.Transaction;
import phoebe.eqx.pcef.enums.ERequestType;
import phoebe.eqx.pcef.enums.state.EState;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.instance.CommitData;
import phoebe.eqx.pcef.message.parser.res.OCFUsageMonitoringResponse;
import phoebe.eqx.pcef.services.GetResourceIdService;
import phoebe.eqx.pcef.services.OCFUsageMonitoringService;
import phoebe.eqx.pcef.services.UsageMonitoringService;
import phoebe.eqx.pcef.services.mogodb.MongoDBConnect;
import phoebe.eqx.pcef.states.abs.ComplexState;
import phoebe.eqx.pcef.states.abs.MessageRecieved;
import phoebe.eqx.pcef.states.mongodb.W_MONGODB_PROCESS_STATE;

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
        MongoDBConnect dbConnect = null;
        EState nextState = null;
        try {
            GetResourceIdService getResourceIdService = new GetResourceIdService(appInstance);
            String resourceId = getResourceIdService.readGetResourceId();

            dbConnect = new MongoDBConnect(appInstance);

            //insert transaction
            dbConnect.getTransactionService().insertTransaction(resourceId);

            //Application Logic wait mongodb process
            W_MONGODB_PROCESS_STATE mongodbProcessState = new W_MONGODB_PROCESS_STATE(appInstance, dbConnect);
            mongodbProcessState.dispatch();

            nextState = mongodbProcessState.getPcefState();
            if (EState.END.equals(nextState)) {
                UsageMonitoringService usageMonitoringService = new UsageMonitoringService(appInstance);
                if (mongodbProcessState.isResponseSuccess()) {
                    usageMonitoringService.buildResponseUsageMonitoring(true);
                } else {
                    throw new Exception("sent error usage monitoring response");
                }
            } else {
                OCFUsageMonitoringService OCFUsageMonitoringService = new OCFUsageMonitoringService(appInstance);
                if (EState.W_USAGE_MONITORING_START.equals(nextState)) {
                    AFLog.d("State is Usage Monitoring First Usage");
                    OCFUsageMonitoringService.buildUsageMonitoringStart(dbConnect);
                } else if (EState.W_USAGE_MONITORING_UPDATE.equals(nextState)) {
                    OCFUsageMonitoringService.buildUsageMonitoringUpdate(dbConnect);
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

            dbConnect.getTransactionService().filterResourceRequestErrorNewResource(ocfUsageMonitoringResponse, appInstance.getMyContext().getPcefInstance().getNewResources(), transactionList);
            ArrayList<Quota> quotaResponseList = dbConnect.getQuotaService().getQuotaFromUsageMonitoringResponse(ocfUsageMonitoringResponse);

            dbConnect.getQuotaService().insertQuotaInitial(quotaResponseList);
            dbConnect.getTransactionService().updateTransaction(quotaResponseList, transactionList);
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
            if (appInstance.getMyContext().getPcefInstance().getCommitDatas().size() == 0) {
                newResourceTransactions.add(thisTransaction); //this transaction new resource
            } else {
                for (CommitData commitData : appInstance.getMyContext().getPcefInstance().getCommitDatas()) {
                    if (commitData.get_id().getResourceId().equals(thisTransaction.getResourceId())) {
                        commitData.getTransactionIds().add(thisTransaction.getTid());//this transaction commit resource
                        break;
                    }
                }
            }
            newResourceTransactions.addAll(appInstance.getMyContext().getPcefInstance().getOtherStartTransactions());

            dbConnect.getTransactionService().filterResourceRequestErrorNewResource(ocfUsageMonitoringResponse, appInstance.getMyContext().getPcefInstance().getNewResources(), newResourceTransactions);
            dbConnect.getTransactionService().filterResourceRequestErrorCommitResource(ocfUsageMonitoringResponse, appInstance.getMyContext().getPcefInstance().getCommitDatas());

            ArrayList<Quota> quotaResponseList = dbConnect.getQuotaService().getQuotaFromUsageMonitoringResponse(ocfUsageMonitoringResponse);

            dbConnect.getQuotaService().updateQuota(quotaResponseList);
            dbConnect.getTransactionService().updateTransaction(quotaResponseList, newResourceTransactions);
            dbConnect.getProfileService().updateProfileUnLock(dbConnect.getQuotaService().isHaveNewQuota(), dbConnect.getQuotaService().getMinExpireDate());

            usageMonitoringService.buildResponseUsageMonitoring(true);
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
