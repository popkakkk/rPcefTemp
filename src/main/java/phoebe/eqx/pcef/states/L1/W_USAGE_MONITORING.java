package phoebe.eqx.pcef.states.L1;

import ec02.af.utils.AFLog;
import phoebe.eqx.pcef.core.model.Quota;
import phoebe.eqx.pcef.core.model.Transaction;
import phoebe.eqx.pcef.enums.state.EState;
import phoebe.eqx.pcef.instance.AppInstance;
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
        GetResourceIdService getResourceIdService = new GetResourceIdService(appInstance);
        String resourceId = getResourceIdService.readGetResourceId();

        MongoDBConnect dbConnect = null;
        EState nextState = null;
        try {
            dbConnect = new MongoDBConnect(appInstance);
            dbConnect.getTransactionService().insertTransaction(resourceId);

            //Application Logic wait mongodb process
            W_MONGODB_PROCESS_STATE mongodbProcessState = new W_MONGODB_PROCESS_STATE(dbConnect);
            mongodbProcessState.dispatch();

            nextState = mongodbProcessState.getUsageMonitoringState();
            if (EState.END.equals(nextState)) {
                UsageMonitoringService usageMonitoringService = new UsageMonitoringService(appInstance);
                if (mongodbProcessState.isResponseSuccess()) {
                    usageMonitoringService.buildResponseUsageMonitoringSuccess();
                } else {
                    usageMonitoringService.buildResponseUsageMonitoringFail();
                }
            } else {
                OCFUsageMonitoringService OCFUsageMonitoringService = new OCFUsageMonitoringService(appInstance);
                if (EState.W_USAGE_MONITORING_START.equals(nextState)) {
                    AFLog.d("State is Usage Monitoring First Usage");
                    List<Transaction> otherTransactionStartList = dbConnect.getTransactionService().findOtherStartTransaction();
                    appInstance.getPcefInstance().getOtherStartTransactions().addAll(otherTransactionStartList);
                    OCFUsageMonitoringService.buildUsageMonitoringStart();
                } else if (EState.W_USAGE_MONITORING_UPDATE.equals(nextState)) {
                    List<Transaction> otherTransactionStartList = dbConnect.getTransactionService().findOtherStartTransaction();
                    dbConnect.getQuotaService().filterTransactionConfirmIsNewResource(otherTransactionStartList);
                    appInstance.getPcefInstance().getOtherStartTransactions().addAll(otherTransactionStartList);
                    OCFUsageMonitoringService.buildUsageMonitoringUpdate();
                }
            }
        } catch (Exception e) {

            AFLog.d("Mongodb initial process error:" + e.getStackTrace()[0]);
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
        OCFUsageMonitoringService umStartService = new OCFUsageMonitoringService(appInstance);
        OCFUsageMonitoringResponse OCFUsageMonitoringResponse = umStartService.readUsageMonitoringStart();

        MongoDBConnect dbConnect = null;
        try {
            dbConnect = new MongoDBConnect(appInstance);
            UsageMonitoringService usageMonitoringService = new UsageMonitoringService(appInstance);
            ArrayList<Quota> quotaResponseList = dbConnect.getQuotaService().getQuotaFromUsageMonitoringResponse(OCFUsageMonitoringResponse);

            if (umStartService.receiveQuotaAndPolicy(OCFUsageMonitoringResponse)) {
                dbConnect.getQuotaService().insertQuotaFirstUsage(quotaResponseList);
                dbConnect.getTransactionService().updateTransaction(quotaResponseList);
                dbConnect.getProfileService().updateProfileUnLockInitial();

                usageMonitoringService.buildResponseUsageMonitoringSuccess();
            } else {
                // update col transaction
                usageMonitoringService.buildResponseUsageMonitoringFail();
            }

        } catch (Exception e) {
            AFLog.d("wUsageMonitoringStart error:" + e.getStackTrace()[0]);
        } finally {
            if (dbConnect != null) {
                dbConnect.closeConnection();
            }

        }
        setWorkState(EState.END);
    }


    @MessageRecieved(messageType = EState.W_USAGE_MONITORING_UPDATE)
    public void wUsageMonitoringUpdate() throws Exception {
        OCFUsageMonitoringService umStartService = new OCFUsageMonitoringService(appInstance);
        OCFUsageMonitoringResponse OCFUsageMonitoringResponse = umStartService.readUsageMonitoringUpdate();

        MongoDBConnect dbConnect = null;
        try {
            dbConnect = new MongoDBConnect(appInstance);
            UsageMonitoringService usageMonitoringService = new UsageMonitoringService(appInstance);
            ArrayList<Quota> quotaResponseList = dbConnect.getQuotaService().getQuotaFromUsageMonitoringResponse(OCFUsageMonitoringResponse);

            if (umStartService.receiveQuotaAndPolicy(OCFUsageMonitoringResponse)) {
                dbConnect.getQuotaService().updateQuota(quotaResponseList);
                dbConnect.getTransactionService().updateTransaction(quotaResponseList);
                dbConnect.getProfileService().updateProfileUnLock();

                usageMonitoringService.buildResponseUsageMonitoringSuccess();
            } else {

                // update col transaction
                usageMonitoringService.buildResponseUsageMonitoringFail();
            }

        } catch (Exception e) {
            AFLog.d("wUsageMonitoringStart error:" + e.getStackTrace()[0]);
        } finally {
            if (dbConnect != null) {
                dbConnect.closeConnection();
            }

        }
        setWorkState(EState.END);

    }
}
