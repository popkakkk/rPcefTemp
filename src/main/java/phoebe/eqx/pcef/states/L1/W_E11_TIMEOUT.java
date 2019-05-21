package phoebe.eqx.pcef.states.L1;

import ec02.af.utils.AFLog;
import phoebe.eqx.pcef.core.model.Transaction;
import phoebe.eqx.pcef.enums.state.EState;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.services.GetResourceIdService;
import phoebe.eqx.pcef.services.OCFUsageMonitoringService;
import phoebe.eqx.pcef.services.UsageMonitoringService;
import phoebe.eqx.pcef.services.mogodb.MongoDBService;
import phoebe.eqx.pcef.states.abs.ComplexState;
import phoebe.eqx.pcef.states.abs.MessageRecieved;
import phoebe.eqx.pcef.states.mongodb.W_MONGODB_PROCESS_E11_VT_TIMEOUT_STATE;

import java.util.List;

public class W_E11_TIMEOUT extends ComplexState {

    public W_E11_TIMEOUT(AppInstance appInstance) {
        super(appInstance, Level.L1);
        this.setState(EState.BEGIN);
    }


    @MessageRecieved(messageType = EState.BEGIN)
    public void begin() throws Exception {
        GetResourceIdService getResourceIdService = new GetResourceIdService(appInstance);
        String resourceId = getResourceIdService.readGetResourceId();

        MongoDBService mongoDBService = null;
        EState nextState = null;
        try {
            mongoDBService = new MongoDBService(appInstance);
            mongoDBService.insertTransaction(resourceId);

            W_MONGODB_PROCESS_E11_VT_TIMEOUT_STATE mongodbProcessState = new W_MONGODB_PROCESS_E11_VT_TIMEOUT_STATE(mongoDBService);
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
                if (EState.W_USAGE_MONITORING_STOP.equals(nextState)) {
                    AFLog.d("State is Usage Monitoring First Usage");
                    List<Transaction> otherTransactionStartList = mongoDBService.findOtherStartTransaction();
                    appInstance.getPcefInstance().getOtherStartTransactions().addAll(otherTransactionStartList);
                    OCFUsageMonitoringService.buildUsageMonitoringStart();
                } else if (EState.W_USAGE_MONITORING_UPDATE.equals(nextState)) {
                    List<Transaction> otherTransactionStartList = mongoDBService.findOtherStartTransaction();
                    mongoDBService.filterTransactionConfirmIsNewResource(otherTransactionStartList);
                    appInstance.getPcefInstance().getOtherStartTransactions().addAll(otherTransactionStartList);
                    OCFUsageMonitoringService.buildUsageMonitoringUpdate();
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


}
