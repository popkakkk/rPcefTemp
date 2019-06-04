package phoebe.eqx.pcef.states.L1;

import ec02.af.utils.AFLog;
import phoebe.eqx.pcef.core.model.Quota;
import phoebe.eqx.pcef.core.model.Transaction;
import phoebe.eqx.pcef.enums.state.EState;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.message.parser.res.OCFUsageMonitoringResponse;
import phoebe.eqx.pcef.services.GenerateCDRService;
import phoebe.eqx.pcef.services.GyRARService;
import phoebe.eqx.pcef.services.OCFUsageMonitoringService;
import phoebe.eqx.pcef.services.mogodb.MongoDBConnect;
import phoebe.eqx.pcef.states.abs.ComplexState;
import phoebe.eqx.pcef.states.abs.MessageRecieved;
import phoebe.eqx.pcef.states.L2.W_MONGODB_PROCESS_GYRAR;

import java.util.ArrayList;
import java.util.List;

public class W_GyRAR extends ComplexState {

    public W_GyRAR(AppInstance appInstance) {
        super(appInstance, Level.L1);
        this.setState(EState.BEGIN);
    }

    @MessageRecieved(messageType = EState.BEGIN)
    public void begin() throws Exception {
        GyRARService gyRARService = new GyRARService(appInstance);
        gyRARService.readGyRAR();

        setWorkState(EState.W_MONGO_PROCESS_GyRAR);
    }

    @MessageRecieved(messageType = EState.W_MONGO_PROCESS_GyRAR)
    public void wMongoGyRAR() throws Exception {
        EState nextState = null;
        GyRARService gyRARService = new GyRARService(appInstance);

        MongoDBConnect dbConnect = null;
        try {
            dbConnect = new MongoDBConnect(appInstance);
            W_MONGODB_PROCESS_GYRAR wMongodbProcessGyrar = new W_MONGODB_PROCESS_GYRAR(appInstance, dbConnect);
            wMongodbProcessGyrar.dispatch();

            nextState = context.getStateL2();
            if (EState.END.equals(context.getStateL3())) {
                if (EState.END.equals(nextState)) {
                    gyRARService.buildResponseGyRAR(false);
                } else {
                    OCFUsageMonitoringService OCFUsageMonitoringService = new OCFUsageMonitoringService(appInstance);
                    OCFUsageMonitoringService.buildUsageMonitoringUpdate(dbConnect);
                }
            }
        } catch (Exception e) {
            AFLog.d(" error:" + e.getStackTrace()[0]);
            throw e;
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
        OCFUsageMonitoringResponse ocfUsageMonitoringResponse = ocfUsageMonitoringService.readUsageMonitoringUpdate();

        MongoDBConnect dbConnect = null;
        try {
            dbConnect = new MongoDBConnect(appInstance);

            List<Transaction> newResourceTransactions = appInstance.getMyContext().getPcefInstance().getOtherStartTransactions();

            dbConnect.getTransactionService().filterResourceRequestErrorNewResource(ocfUsageMonitoringResponse, appInstance.getMyContext().getPcefInstance().getNewResourcesRequests(), newResourceTransactions);
            dbConnect.getTransactionService().filterResourceRequestErrorCommitResource(ocfUsageMonitoringResponse, appInstance.getMyContext().getPcefInstance().getCommitDatas());

            ArrayList<Quota> quotaResponseList = dbConnect.getQuotaService().getQuotaFromUsageMonitoringResponse(ocfUsageMonitoringResponse);
            dbConnect.getQuotaService().insertQuotaInitial(quotaResponseList);
            dbConnect.getTransactionService().updateTransaction(quotaResponseList, newResourceTransactions);

            GenerateCDRService generateCDRService = new GenerateCDRService();
            generateCDRService.buildCDRCharging(newResourceTransactions, appInstance.getAbstractAF());

            dbConnect.getProfileService().updateProfileUnLock(dbConnect.getQuotaService().isHaveNewQuota(), dbConnect.getQuotaService().getMinExpireDate());

            GyRARService gyRARService = new GyRARService(appInstance);
            gyRARService.buildResponseGyRAR(true);


        } catch (Exception e) {
            AFLog.d(" error:" + e.getStackTrace()[0]);
            throw e;
        } finally {
            if (dbConnect != null) {
                dbConnect.closeConnection();
            }

        }
        setWorkState(EState.END);
    }


}
