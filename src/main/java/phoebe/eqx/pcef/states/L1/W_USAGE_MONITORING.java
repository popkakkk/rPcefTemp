package phoebe.eqx.pcef.states.L1;

import ec02.af.utils.AFLog;
import phoebe.eqx.pcef.core.data.OCFUsageMonitoring;
import phoebe.eqx.pcef.enums.state.EState;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.model.Quota;
import phoebe.eqx.pcef.services.mogodb.MongoDBService;
import phoebe.eqx.pcef.services.ocf.UsageMonitoringService;
import phoebe.eqx.pcef.services.product.GetResourceIdService;
import phoebe.eqx.pcef.states.mongodb.W_MONGODB_PROCESS_STATE;
import phoebe.eqx.pcef.states.abs.ComplexState;
import phoebe.eqx.pcef.states.abs.MessageRecieved;

import java.util.ArrayList;

public class W_USAGE_MONITORING extends ComplexState {

    public W_USAGE_MONITORING(AppInstance appInstance) {
        super(appInstance, Level.L1);
        this.setState(EState.BEGIN);
    }

    @MessageRecieved(messageType = EState.BEGIN)
    public void begin() throws Exception {
        phoebe.eqx.pcef.services.sacf.UsageMonitoringService usageMonitoringService = new phoebe.eqx.pcef.services.sacf.UsageMonitoringService(appInstance);
        usageMonitoringService.readUsageMonitoringRequest();

        GetResourceIdService getResourceIdService = new GetResourceIdService(appInstance);
        getResourceIdService.buildGetResourceId();
        setWorkState(EState.W_GET_RESOURCE_ID);
    }

    @MessageRecieved(messageType = EState.W_GET_RESOURCE_ID)
    public void wGetResourceId() throws Exception {
        GetResourceIdService getResourceIdService = new GetResourceIdService(appInstance);
        String resourceId = getResourceIdService.readGetResourceId();

        MongoDBService mongoDBService = null;
        EState nextState = null;
        try {
            mongoDBService = new MongoDBService(appInstance);
            mongoDBService.insertTransaction(resourceId);

            //Application Logic wait mongodb process
            W_MONGODB_PROCESS_STATE mongodbProcessState = new W_MONGODB_PROCESS_STATE(mongoDBService);
            mongodbProcessState.dispatch();

            nextState = mongodbProcessState.getUsageMonitoringState();
            if (EState.END.equals(nextState)) {
                phoebe.eqx.pcef.services.sacf.UsageMonitoringService usageMonitoringService = new phoebe.eqx.pcef.services.sacf.UsageMonitoringService(appInstance);
                if (mongodbProcessState.isResponseSuccess()) {
                    usageMonitoringService.buildResponseUsageMonitoringSuccess();
                } else {
                    usageMonitoringService.buildResponseUsageMonitoringFail();
                }
            } else {
                UsageMonitoringService UsageMonitoringService = new UsageMonitoringService(appInstance);
                if (EState.W_USAGE_MONITORING_START.equals(nextState)) {
                    AFLog.d("State is Usage Monitoring First Usage");
                    mongoDBService.findOtherStartTransaction();
                    UsageMonitoringService.buildUsageMonitoringStart();
                } else if (EState.W_USAGE_MONITORING_UPDATE.equals(nextState)) {
                    mongoDBService.findOtherStartTransaction();
                    UsageMonitoringService.buildUsageMonitoringUpdate();
                }
            }
        } catch (Exception e) {


        } finally {
            if (mongoDBService != null) {
                mongoDBService.closeConnection();
            }
            if (nextState == null) {
                nextState = EState.END;
            }
        }

        setWorkState(nextState);
    }


    @MessageRecieved(messageType = EState.W_USAGE_MONITORING_START)
    public void wUsageMonitoringStart() throws Exception {
        UsageMonitoringService umStartService = new UsageMonitoringService(appInstance);
        OCFUsageMonitoring usageMonitoringResponse = umStartService.readUsageMonitoringStart();

        MongoDBService mongoDBService = null;
        try {
            mongoDBService = new MongoDBService(appInstance);
            phoebe.eqx.pcef.services.sacf.UsageMonitoringService usageMonitoringService = new phoebe.eqx.pcef.services.sacf.UsageMonitoringService(appInstance);
            ArrayList<Quota> quotaResponseList = mongoDBService.getQuotaFromUsageMonitoringResponse(usageMonitoringResponse);

            if (umStartService.receiveQuotaAndPolicy()) {
                mongoDBService.insertQuotaStartFirstUsage(quotaResponseList);
                mongoDBService.updateTransactionSetQuota(quotaResponseList);
                mongoDBService.updateProfileUnLockInitial();

                usageMonitoringService.buildResponseUsageMonitoringSuccess();
            } else {

                // update col transaction
                usageMonitoringService.buildResponseUsageMonitoringFail();
            }

        } catch (Exception e) {
            AFLog.d("wUsageMonitoringStart error:" + e.toString());
        } finally {
            if (mongoDBService != null) {
                mongoDBService.closeConnection();
            }

        }
        setWorkState(EState.END);
    }


    @MessageRecieved(messageType = EState.W_USAGE_MONITORING_UPDATE)
    public void wUsageMonitoringUpdate() throws Exception {
        UsageMonitoringService umStartService = new UsageMonitoringService(appInstance);
        OCFUsageMonitoring usageMonitoringResponse = umStartService.readUsageMonitoringUpdate();

        MongoDBService mongoDBService = null;
        try {
            mongoDBService = new MongoDBService(appInstance);
            phoebe.eqx.pcef.services.sacf.UsageMonitoringService usageMonitoringService = new phoebe.eqx.pcef.services.sacf.UsageMonitoringService(appInstance);
            ArrayList<Quota> quotaResponseList = mongoDBService.getQuotaFromUsageMonitoringResponse(usageMonitoringResponse);

            if (umStartService.receiveQuotaAndPolicy()) {
                mongoDBService.updateQuota(quotaResponseList);
                mongoDBService.updateTransactionSetQuota(quotaResponseList);
                mongoDBService.updateProfileUnLock();

                usageMonitoringService.buildResponseUsageMonitoringSuccess();
            } else {

                // update col transaction
                usageMonitoringService.buildResponseUsageMonitoringFail();
            }

        } catch (Exception e) {
            AFLog.d("wUsageMonitoringStart error:" + e.toString());
        } finally {
            if (mongoDBService != null) {
                mongoDBService.closeConnection();
            }

        }
        setWorkState(EState.END);

    }
}
