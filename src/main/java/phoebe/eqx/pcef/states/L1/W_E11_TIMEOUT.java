package phoebe.eqx.pcef.states.L1;

import phoebe.eqx.pcef.enums.state.EState;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.services.OCFUsageMonitoringService;
import phoebe.eqx.pcef.services.UsageMonitoringService;
import phoebe.eqx.pcef.services.mogodb.MongoDBConnect;
import phoebe.eqx.pcef.states.abs.ComplexState;
import phoebe.eqx.pcef.states.abs.MessageRecieved;
import phoebe.eqx.pcef.states.mongodb.W_MONGODB_PROCESS_E11_VT_TIMEOUT_STATE;

public class W_E11_TIMEOUT extends ComplexState {

    public W_E11_TIMEOUT(AppInstance appInstance) {
        super(appInstance, Level.L1);
        this.setState(EState.BEGIN);
    }


    @MessageRecieved(messageType = EState.BEGIN)
    public void begin() throws Exception {
        MongoDBConnect dbConnect = null;
        EState nextState = null;
        try {
            dbConnect = new MongoDBConnect(appInstance);

            W_MONGODB_PROCESS_E11_VT_TIMEOUT_STATE mongodbProcessState = new W_MONGODB_PROCESS_E11_VT_TIMEOUT_STATE(dbConnect);
            mongodbProcessState.dispatch();

            nextState = mongodbProcessState.getUsageMonitoringState();
            if (EState.END.equals(nextState)) {
                UsageMonitoringService usageMonitoringService = new UsageMonitoringService(appInstance);
                if (mongodbProcessState.isResponseSuccess()) {

                } else {

                }
            } else {
                OCFUsageMonitoringService OCFUsageMonitoringService = new OCFUsageMonitoringService(appInstance);
                if (EState.W_USAGE_MONITORING_STOP.equals(nextState)) {

                } else if (EState.W_USAGE_MONITORING_UPDATE.equals(nextState)) {

                }
            }
        } catch (Exception e) {


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


}
