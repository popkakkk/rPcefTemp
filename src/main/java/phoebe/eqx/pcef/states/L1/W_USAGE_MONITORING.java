package phoebe.eqx.pcef.states.L1;

import com.mongodb.DBCursor;
import phoebe.eqx.pcef.enums.EState;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.instance.Config;
import phoebe.eqx.pcef.services.MongoDBService;
import phoebe.eqx.pcef.services.SACFService;
import phoebe.eqx.pcef.services.SDFService;
import phoebe.eqx.pcef.services.UsageMonitoringStartService;
import phoebe.eqx.pcef.states.abs.ComplexState;
import phoebe.eqx.pcef.states.abs.MessageRecieved;

public class W_USAGE_MONITORING extends ComplexState {

    public W_USAGE_MONITORING(AppInstance appInstance) {
        super(appInstance, Level.L1);
        this.setState(EState.BEGIN);
    }

    @MessageRecieved(messageType = EState.BEGIN)
    public void begin() throws Exception {
        SACFService sacfService = new SACFService(appInstance);
        sacfService.readRequest();

        SDFService sdfService = new SDFService(appInstance);
        sdfService.buildGetResourceId();
        setWorkState(EState.W_GET_RESOURCE_ID);
    }

    @MessageRecieved(messageType = EState.W_GET_RESOURCE_ID)
    public void wGetResourceId() throws Exception {
        SDFService sdfService = new SDFService(appInstance);
        String resourceId = sdfService.readGetResourceId();

        MongoDBService mongoDBService = new MongoDBService(appInstance);
        mongoDBService.insertTransaction(resourceId);

        EState nextState;
        DBCursor lockProcessCursor = mongoDBService.findLockProcess();
        if (!lockProcessCursor.hasNext()) {
            mongoDBService.insertLockProcess();

            UsageMonitoringStartService usageMonitoringStartService = new UsageMonitoringStartService(appInstance);
            usageMonitoringStartService.buildUsageMonitoringStart();

            nextState = EState.W_USAGE_MONITORING_START;
        } else {
            boolean canProcess = mongoDBService.checkCanProcess(lockProcessCursor);
            if (!canProcess) {
                mongoDBService.waitIntervalIsProcessing();
            }




            nextState = EState.W_USAGE_MONITORING_UPDATE;
        }


        if (EState.W_USAGE_MONITORING_START.equals(nextState)) {

        }
        setWorkState(nextState);
    }

    @MessageRecieved(messageType = EState.W_USAGE_MONITORING_START)
    public void wUsageMonitoringStart() throws Exception {
        UsageMonitoringStartService usageMonitoringStartService = new UsageMonitoringStartService(appInstance);
        usageMonitoringStartService.readUsageMonitoringStart();

        //Receive Quota and Policy
        MongoDBService mongoDBService = new MongoDBService(appInstance);
        mongoDBService.insertQuota();

        //update col transaction
        mongoDBService.updateMonitoringKeyTransaction();

        //update col lockprocess
        mongoDBService.updateProcessingLockProcess();

        SACFService sacfService = new SACFService(appInstance);
        sacfService.buildResponseSACFSuccess();
        setWorkState(EState.END);

    }


}
