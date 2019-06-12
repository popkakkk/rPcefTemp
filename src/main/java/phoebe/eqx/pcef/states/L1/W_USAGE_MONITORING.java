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
import phoebe.eqx.pcef.states.L2.W_MOD_PROCESS;
import phoebe.eqx.pcef.states.L2.W_MONGODB_PROCESS_USAGE_MONITORING;
import phoebe.eqx.pcef.states.abs.ComplexState;
import phoebe.eqx.pcef.states.abs.MessageRecieved;

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
        getResourceIdService.readGetResourceId();
        setWorkState(EState.W_MONGO_PROCESS_USAGE_MONITORING);
    }

    @MessageRecieved(messageType = EState.W_MONGO_PROCESS_USAGE_MONITORING)
    public void wMongoUsageMonitoring() throws Exception {
        MongoDBConnect dbConnect = null;
        EState nextState = null;

        try {
            dbConnect = new MongoDBConnect(appInstance);

            //Application Logic wait mongodb process
            W_MONGODB_PROCESS_USAGE_MONITORING mongodbProcessState = new W_MONGODB_PROCESS_USAGE_MONITORING(appInstance, dbConnect);
            mongodbProcessState.dispatch();

            nextState = context.getStateL2();
            if (EState.END.equals(context.getStateL3())) {
                if (EState.END.equals(nextState)) {
                    UsageMonitoringService usageMonitoringService = new UsageMonitoringService(appInstance);
                    if (mongodbProcessState.isResponseSuccess()) {
                        usageMonitoringService.buildResponseUsageMonitoring(true);
                    } else {
                        usageMonitoringService.buildResponseUsageMonitoring(false);
                    }
                } else {
                    OCFUsageMonitoringService ocfUsageMonitoringService = new OCFUsageMonitoringService(appInstance);
                    if (EState.W_USAGE_MONITORING_START.equals(nextState)) {
                        AFLog.d("State is Usage Monitoring First Usage");
                        ocfUsageMonitoringService.buildUsageMonitoringStart(dbConnect);
                    } else if (EState.W_USAGE_MONITORING_UPDATE.equals(nextState)) {
                        ocfUsageMonitoringService.buildUsageMonitoringUpdate(dbConnect);
                    }
                }
            }
        } catch (Exception e) {
            if (dbConnect != null) {
                dbConnect.getTransactionService().deleteTransactionError();
            }

            AFLog.d("Mongodb initial process error:" + e.getStackTrace()[0]);
            throw e;
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
        OCFUsageMonitoringService ocfUsageMonitoringService = new OCFUsageMonitoringService(appInstance);
        OCFUsageMonitoringResponse ocfUsageMonitoringResponse = ocfUsageMonitoringService.readUsageMonitoringStart();
        ArrayList<Quota> quotaResponseList = ocfUsageMonitoringService.getQuotaFromUsageMonitoringResponse(ocfUsageMonitoringResponse);

        MongoDBConnect dbConnect = null;
        try {

            dbConnect = new MongoDBConnect(appInstance);
            UsageMonitoringService usageMonitoringService = new UsageMonitoringService(appInstance);

            List<Transaction> transactionList = new ArrayList<>();
            transactionList.add(appInstance.getMyContext().getPcefInstance().getTransaction());
            transactionList.addAll(appInstance.getMyContext().getPcefInstance().getOtherStartTransactions());

            ocfUsageMonitoringService.processFirstUsage(dbConnect, ocfUsageMonitoringResponse, quotaResponseList, transactionList);
            dbConnect.getProfileService().updateProfileUnLockInitial(dbConnect.getQuotaService().getMinExpireDate());

            //check My Transaction Error
            boolean transactionError = true;
            for (Transaction t : transactionList) {
                if (t.getResourceId().equals(context.getPcefInstance().getTransaction().getResourceId())) {
                    transactionError = false;
                    break;
                }
            }

            if (!transactionError) {
                usageMonitoringService.buildResponseUsageMonitoring(true);
            } else {
                usageMonitoringService.buildResponseUsageMonitoring(false);
            }

        } catch (Exception e) {
            AFLog.d("wUsageMonitoringStart error:" + e.getStackTrace()[0]);
            if (dbConnect != null) {
                dbConnect.getTransactionService().deleteTransactionError();
            }
            throw e;
        } finally {
            if (dbConnect != null) {
                dbConnect.closeConnection();
            }

        }
        setWorkState(EState.END);
    }


    @MessageRecieved(messageType = EState.W_USAGE_MONITORING_UPDATE)
    public void wUsageMonitoringUpdate() throws Exception {
        OCFUsageMonitoringService ocfUsageMonitoringService = new OCFUsageMonitoringService(appInstance);
        OCFUsageMonitoringResponse ocfUsageMonitoringResponse = ocfUsageMonitoringService.readUsageMonitoringUpdate();
        ArrayList<Quota> quotaResponseList = ocfUsageMonitoringService.getQuotaFromUsageMonitoringResponse(ocfUsageMonitoringResponse);

        EState nextState = null;

        MongoDBConnect dbConnect = null;
        try {
            dbConnect = new MongoDBConnect(appInstance);
            UsageMonitoringService usageMonitoringService = new UsageMonitoringService(appInstance);

            List<Transaction> newResourceTransactions = new ArrayList<>();

            Transaction thisTransaction = appInstance.getMyContext().getPcefInstance().getTransaction();

            newResourceTransactions.add(thisTransaction); //this transaction new resource
            newResourceTransactions.addAll(appInstance.getMyContext().getPcefInstance().getOtherStartTransactions());

            //filter


            ocfUsageMonitoringService.processUpdate(dbConnect, ocfUsageMonitoringResponse, quotaResponseList, newResourceTransactions);

            //check My Transaction Error
            boolean transactionError = true;
            for (Transaction t : newResourceTransactions) {
                if (t.getResourceId().equals(thisTransaction.getResourceId())) {
                    transactionError = false;
                    break;
                }
            }


            dbConnect.getProfileService().updateProfileUnLock(dbConnect.getQuotaService().isHaveNewQuota(), dbConnect.getQuotaService().getMinExpireDate());

            //check Have new Commit Data
            if (context.getPcefInstance().getCommitDataNewList().size() > 0) {
                nextState = EState.W_MONGO_PROCESS_USAGE_MONITORING_MOD_PROCESS;
            } else {
                if (!transactionError) {
                    usageMonitoringService.buildResponseUsageMonitoring(true);
                } else {
                    usageMonitoringService.buildResponseUsageMonitoring(false);
                }
                nextState = EState.END;
            }


        } catch (Exception e) {
            AFLog.d("wUsageMonitoringUpdate error -" + e.getStackTrace()[0]);

            if (dbConnect != null) {
                dbConnect.getTransactionService().deleteTransactionError();
            }
            throw e;
        } finally {
            if (dbConnect != null) {
                dbConnect.closeConnection();
            }

        }
        setWorkState(nextState);
    }


    @MessageRecieved(messageType = EState.W_MONGO_PROCESS_USAGE_MONITORING_MOD_PROCESS)
    public void modProcess() throws Exception {
        MongoDBConnect dbConnect = null;
        EState nextState = null;

        try {
            dbConnect = new MongoDBConnect(appInstance);

            //Application Logic wait mongodb process
            W_MOD_PROCESS wModProcess = new W_MOD_PROCESS(appInstance, dbConnect);
            wModProcess.dispatch();

            nextState = context.getStateL2();
            if (EState.END.equals(context.getStateL3())) {
                if (EState.END.equals(nextState)) {
                    UsageMonitoringService usageMonitoringService = new UsageMonitoringService(appInstance);
                    if (wModProcess.isResponseSuccess()) {
                        usageMonitoringService.buildResponseUsageMonitoring(true);
                    } else {
                        usageMonitoringService.buildResponseUsageMonitoring(false);
                    }
                } else {
                    OCFUsageMonitoringService ocfUsageMonitoringService = new OCFUsageMonitoringService(appInstance);
                    if (EState.W_USAGE_MONITORING_UPDATE.equals(nextState)) {
                        ocfUsageMonitoringService.buildUsageMonitoringUpdate(dbConnect);
                    }
                }
            }
        } catch (Exception e) {
            if (dbConnect != null) {
                dbConnect.getTransactionService().deleteTransactionError();
            }

            AFLog.d("mod Process error:" + e.getStackTrace()[0]);
            throw e;
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
