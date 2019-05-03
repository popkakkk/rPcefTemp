package phoebe.eqx.pcef.states.L1;

import com.mongodb.DBCursor;
import phoebe.eqx.pcef.enums.EState;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.services.*;
import phoebe.eqx.pcef.states.mongodb.W_MONGODB_PROCESS_STATE;
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
        sdfService.readGetResourceId();

        MongoDBService mongoDBService = null;
        EState nextState = null;
        try {
            mongoDBService = new MongoDBService(appInstance);
            mongoDBService.insertTransaction();

            W_MONGODB_PROCESS_STATE mongodbProcessState = new W_MONGODB_PROCESS_STATE(mongoDBService);
            mongodbProcessState.dispatch();

            nextState = mongodbProcessState.getPcefNextState();
            if (EState.END.equals(nextState)) {
                SACFService sacfService = new SACFService(appInstance);
                if (mongodbProcessState.isResponseSuccess()) {
                    sacfService.buildResponseSACFSuccess();
                } else {
                    sacfService.buildResponseSACFFail();
                }
            } else {
                UsageMonitoringService usageMonitoringService = new UsageMonitoringService(appInstance);
                if (EState.W_USAGE_MONITORING_START.equals(nextState)) {

                    DBCursor transactionCursor = mongoDBService.findTransactionForFirstUsage();
                    usageMonitoringService.buildUsageMonitoringStart();
                } else if (EState.W_USAGE_MONITORING_UPDATE.equals(nextState)) {
                    usageMonitoringService.buildUsageMonitoringUpdate();
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
        UsageMonitoringService usageMonitoringStartService = new UsageMonitoringService(appInstance);
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
