package phoebe.eqx.pcef.states.L1;

import ec02.af.utils.AFLog;
import phoebe.eqx.pcef.core.model.Quota;
import phoebe.eqx.pcef.enums.state.EState;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.message.parser.res.OCFUsageMonitoringResponse;
import phoebe.eqx.pcef.services.OCFUsageMonitoringService;
import phoebe.eqx.pcef.services.VTTimoutService;
import phoebe.eqx.pcef.services.mogodb.MongoDBConnect;
import phoebe.eqx.pcef.states.abs.ComplexState;
import phoebe.eqx.pcef.states.abs.MessageRecieved;
import phoebe.eqx.pcef.states.mongodb.W_MONGODB_PROCESS_E11_VT_TIMEOUT_STATE;

import java.util.ArrayList;

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

            W_MONGODB_PROCESS_E11_VT_TIMEOUT_STATE mongodbProcessState = new W_MONGODB_PROCESS_E11_VT_TIMEOUT_STATE(appInstance, dbConnect);
            mongodbProcessState.dispatch();

            nextState = mongodbProcessState.getPcefState();
            if (EState.END.equals(nextState)) {
                // set Profile appointmentDate = (now - appointment)
            } else {
                OCFUsageMonitoringService OCFUsageMonitoringService = new OCFUsageMonitoringService(appInstance);
                if (EState.W_USAGE_MONITORING_STOP.equals(nextState)) {
                    //build stop
                    OCFUsageMonitoringService.buildUsageMonitoringStop();

                } else if (EState.W_USAGE_MONITORING_UPDATE.equals(nextState)) {
                    //check resource used  rr0 rr1
                    OCFUsageMonitoringService.buildUsageMonitoringUpdate(dbConnect);
                }
            }
        } catch (Exception e) {
            AFLog.d("error:" + e.getStackTrace()[0]);

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

                VTTimoutService vtTimoutService = new VTTimoutService(appInstance);
                vtTimoutService.buildRecurringTimout();

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


    @MessageRecieved(messageType = EState.W_USAGE_MONITORING_STOP)
    public void wUsageMonitoringStop() throws Exception {
        MongoDBConnect dbConnect = null;
        OCFUsageMonitoringService ocfUsageMonitoringService = new OCFUsageMonitoringService(appInstance);
        ocfUsageMonitoringService.readUsageMonitoringStop();

        try {
            dbConnect = new MongoDBConnect(appInstance);

            String privateId = appInstance.getPcefInstance().getProfile().getUserValue();

            //stop by privateId
            dbConnect.getTransactionService().updateTransactionIsActive(privateId);
            dbConnect.getQuotaService().removeQuota(privateId);
            dbConnect.getProfileService().removeProfile(privateId);

            //end
            //finish ret 10


        } catch (Exception e) {


        } finally {
            if (dbConnect != null) {
                dbConnect.closeConnection();
            }
        }
        setWorkState(EState.END);
    }


}
