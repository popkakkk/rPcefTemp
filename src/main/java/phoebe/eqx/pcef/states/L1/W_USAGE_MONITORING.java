package phoebe.eqx.pcef.states.L1;

import phoebe.eqx.pcef.enums.EState;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.services.*;
import phoebe.eqx.pcef.states.L2.MongoDBFirstState;
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

        MongoDBService2 mgService = null;
        EState nextState = null;
        try {
            mgService = new MongoDBService2(appInstance);
            MongoDBFirstState mongoDBFirstState = new MongoDBFirstState(mgService);
            mongoDBFirstState.start();

            if (mongoDBFirstState.getStatus().equalsIgnoreCase("Success")) {

                if (mongoDBFirstState.getNextState() != null) {
                    nextState = mongoDBFirstState.getNextState();
                    if (EState.W_USAGE_MONITORING_START.equals(nextState)) {
                        //sent usageMonitoring
                        UsageMonitoringService usageMonitoringStartService = new UsageMonitoringService(appInstance);
                        usageMonitoringStartService.buildUsageMonitoringStart();
                    } else if (EState.W_USAGE_MONITORING_UPDATE.equals(nextState)) {

                    }


                } else {
                    //build response success 200
                }
            } else {
                //build response error 500
            }


        } catch (Exception e) {


        } finally {
            if (mgService != null) {
                mgService.closeConnection();
            }
        }

        setWorkState(nextState);
    }

    /* @MessageRecieved(messageType = EState.W_GET_RESOURCE_ID)
    public void wGetResourceId() throws Exception {
        SDFService sdfService = new SDFService(appInstance);
        String resourceId = sdfService.readGetResourceId();

        MongoDBService mgService = null;
        EState nextState = null;
        try {
            mgService = new MongoDBService(appInstance);
            mgService.insertTransaction(resourceId);

            boolean isStartState = mgService.checkIsUsageMonitoringStartState();
            if (isStartState) {
                //find Col_Transaction
                //#if(is not transaction) set status=Processing

                //sent usageMonitoring
                UsageMonitoringService usageMonitoringStartService = new UsageMonitoringService(appInstance);
                usageMonitoringStartService.buildUsageMonitoringStart();

                nextState = EState.W_USAGE_MONITORING_START;
            } else {




                nextState = EState.W_USAGE_MONITORING_UPDATE;
            }


        } catch (Exception e) {


        } finally {
            if (mgService != null) {
                mgService.closeConnection();
            }
        }

        setWorkState(nextState);
    }*/


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
