package phoebe.eqx.pcef.states.L1;

import ec02.af.utils.AFLog;
import phoebe.eqx.pcef.core.model.Quota;
import phoebe.eqx.pcef.enums.state.EState;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.message.parser.res.OCFUsageMonitoringResponse;
import phoebe.eqx.pcef.services.GyRARService;
import phoebe.eqx.pcef.services.OCFUsageMonitoringService;
import phoebe.eqx.pcef.services.mogodb.MongoDBConnect;
import phoebe.eqx.pcef.states.abs.ComplexState;
import phoebe.eqx.pcef.states.abs.MessageRecieved;
import phoebe.eqx.pcef.states.mongodb.W_MONGODB_PROCESS_GYRAR;

import java.util.ArrayList;

public class W_GyRAR extends ComplexState {

    public W_GyRAR(AppInstance appInstance) {
        super(appInstance, Level.L1);
        this.setState(EState.BEGIN);
    }

    @MessageRecieved(messageType = EState.BEGIN)
    public void begin() {
        EState nextState = null;

        GyRARService gyRARService = new GyRARService(appInstance);
        gyRARService.readGyRAR();

        MongoDBConnect dbConnect = null;
        try {
            dbConnect = new MongoDBConnect(appInstance);
            W_MONGODB_PROCESS_GYRAR wMongodbProcessGyrar = new W_MONGODB_PROCESS_GYRAR(appInstance, dbConnect);
            wMongodbProcessGyrar.dispatch();

            nextState = wMongodbProcessGyrar.getUsageMonitoringState();
            if (EState.END.equals(nextState)) {
                //end
            } else {
                OCFUsageMonitoringService OCFUsageMonitoringService = new OCFUsageMonitoringService(appInstance);
                OCFUsageMonitoringService.buildUsageMonitoringUpdate(dbConnect);
            }
        } catch (Exception e) {
            AFLog.d(" error:" + e.getStackTrace()[0]);
        } finally {
            if (dbConnect != null) {
                dbConnect.closeConnection();
            }
        }
        setWorkState(nextState);
    }


    @MessageRecieved(messageType = EState.W_USAGE_MONITORING_UPDATE)
    public void wUsageMonitoringUpdate() throws Exception {
        OCFUsageMonitoringService ocfUsageMonitoringService = new OCFUsageMonitoringService(appInstance);
        OCFUsageMonitoringResponse OCFUsageMonitoringResponse = ocfUsageMonitoringService.readUsageMonitoringUpdate();

        MongoDBConnect dbConnect = null;
        try {
            dbConnect = new MongoDBConnect(appInstance);
            ArrayList<Quota> quotaResponseList = dbConnect.getQuotaService().getQuotaFromUsageMonitoringResponse(OCFUsageMonitoringResponse);

            if (ocfUsageMonitoringService.receiveQuotaAndPolicy(OCFUsageMonitoringResponse)) {
                dbConnect.getQuotaService().updateQuota(quotaResponseList);
                dbConnect.getTransactionService().updateTransaction(quotaResponseList);
                dbConnect.getProfileService().updateProfileUnLock(dbConnect.getQuotaService().isHaveNewQuota(), dbConnect.getQuotaService().getMinExpireDate());

                GyRARService gyRARService = new GyRARService(appInstance);
                gyRARService.buildResponseGyRAR();

            } else {


            }

        } catch (Exception e) {
            AFLog.d(" error:" + e.getStackTrace()[0]);
        } finally {
            if (dbConnect != null) {
                dbConnect.closeConnection();
            }

        }
        setWorkState(EState.END);
    }

}
