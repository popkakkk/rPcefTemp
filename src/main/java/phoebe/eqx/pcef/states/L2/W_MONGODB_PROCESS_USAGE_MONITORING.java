package phoebe.eqx.pcef.states.L2;


import com.google.gson.Gson;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.DuplicateKeyException;
import ec02.af.utils.AFLog;
import phoebe.eqx.pcef.core.exceptions.TimeoutIntervalException;
import phoebe.eqx.pcef.core.model.Quota;
import phoebe.eqx.pcef.core.model.Transaction;
import phoebe.eqx.pcef.enums.state.EState;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.instance.CommitData;
import phoebe.eqx.pcef.services.E11TimoutService;
import phoebe.eqx.pcef.services.mogodb.MongoDBConnect;
import phoebe.eqx.pcef.states.abs.MessageRecieved;
import phoebe.eqx.pcef.states.abs.MongoState;

import java.util.ArrayList;
import java.util.List;

public class W_MONGODB_PROCESS_USAGE_MONITORING extends MongoState {


    public W_MONGODB_PROCESS_USAGE_MONITORING(AppInstance appInstance, MongoDBConnect mongoDBConnect) {
        super(appInstance, Level.L2, mongoDBConnect);
    }


    @MessageRecieved(messageType = EState.BEGIN)
    public void InitialProcess() {
        EState nextState = null;
        try {
            dbConnect.getTransactionService().insertTransaction(context.getPcefInstance().getResourceId());

            DBCursor profileCursor = dbConnect.getProfileService().findProfileByPrivateId(context.getPcefInstance().getUsageMonitoringRequest().getUserValue());
            if (!profileCursor.hasNext()) {
                nextState = EState.INSERT_PROFILE;
            } else {
                nextState = EState.CHECK_QUOTA_AVAILIABLE;
            }
        } catch (Exception e) {
            setResponseFail();
            nextState = EState.END;
        }
        setWorkState(nextState);
    }


    @MessageRecieved(messageType = EState.INSERT_PROFILE)
    public void insertProfileState() {
        EState nextState;
        try {
            dbConnect.getProfileService().insertProfile();
            setState(EState.W_USAGE_MONITORING_START);
            nextState = EState.END;
        } catch (DuplicateKeyException e) {
            nextState = EState.CHECK_QUOTA_AVAILIABLE;
        } catch (Exception e) {
            try {
                E11TimoutService e11TimoutService = new E11TimoutService(appInstance);
                e11TimoutService.buildInterval();

                nextState = EState.INSERT_PROFILE;
            } catch (TimeoutIntervalException ex) {
                setResponseFail();
                nextState = EState.END;
            }

        }
        setWorkState(nextState);
    }

    @MessageRecieved(messageType = EState.CHECK_QUOTA_AVAILIABLE)
    public void checkQuotaAvailable() {
        EState nextState = null;
        context.setWaitForProcess(false);
        try {
            DBCursor QuotaCursor = dbConnect.getQuotaService().findQuotaByTransaction(context.getPcefInstance().getTransaction());
            if (!QuotaCursor.hasNext()) {
                if (context.isLockByMyTransaction()) {
                    setState(EState.W_USAGE_MONITORING_UPDATE);
                    nextState = EState.END;
                } else {
                    nextState = EState.FIND_AND_MOD_PROFILE_FOR_WAIT_PROCESS;
                }
            } else {
                if (dbConnect.getQuotaService().checkMkCanProcess(QuotaCursor)) {
                    Quota quota = new Gson().fromJson(new Gson().toJson(QuotaCursor.iterator().next()), Quota.class);

                    /*----------------- [Check Quota Exhaust]------------------------------------*/
                    //count transaction
                    List<CommitData> commitDataList = dbConnect.getQuotaService().findDataToCommit(quota.getUserValue(), quota.getMonitoringKey(), false);

                    //count resourceId this transaction + 1
                    for (CommitData commitData : commitDataList) {
                        if (commitData.get_id().getResourceId().equals(context.getPcefInstance().getTransaction().getResourceId())) {
                            commitData.setCount(commitData.getCount() + 1);
                            break;
                        }
                    }

                    int sumTransaction = commitDataList.stream().mapToInt(CommitData::getCount).sum();
                    int quotaUnit = quota.getQuotaByKey().getUnit();

                    AFLog.d("sum transaction unit:" + sumTransaction);
                    AFLog.d("quota unit:" + quotaUnit);

                    if (quotaUnit > sumTransaction) {
                        AFLog.d("Quota Available");

                        ArrayList<Quota> myQuotaToList = new ArrayList<>();
                        myQuotaToList.add(quota);

                        List<Transaction> transactionList = new ArrayList<>();
                        transactionList.add(context.getPcefInstance().getTransaction());

                        //update transaction
                        dbConnect.getTransactionService().updateTransaction(myQuotaToList, transactionList);

                        AFLog.d("State is Usage Monitoring Usage with available quota");
                        setResponseSuccess();
                        nextState = EState.END;
                    } else {
                        AFLog.d("Quota Exhaust");
                        context.getPcefInstance().setCommitDatas(commitDataList);
                        DBObject findModQuota = dbConnect.getQuotaService().findAndModifyLockQuota(quota.getMonitoringKey());
                        if (findModQuota != null) {
                            nextState = EState.FIND_AND_MOD_PROFILE_FOR_UPDATE_QUOTA_EXHAUST;
                        } else {
                            E11TimoutService e11TimoutService = new E11TimoutService(appInstance);
                            e11TimoutService.buildInterval();

                            nextState = EState.CHECK_QUOTA_AVAILIABLE;
                        }
                    }
                } else {
                    E11TimoutService e11TimoutService = new E11TimoutService(appInstance);
                    e11TimoutService.buildInterval();

                    nextState = EState.CHECK_QUOTA_AVAILIABLE;
                }
            }
        } catch (Exception e) {
            setResponseFail();
            nextState = EState.END;

        }
        setWorkState(nextState);
    }

    @MessageRecieved(messageType = EState.FIND_AND_MOD_PROFILE_FOR_WAIT_PROCESS)
    public void findAndModProfileForWaitProcess() {
        EState nextState = null;
        try {

            DBObject dbObject = dbConnect.getProfileService().findAndModifyLockProfile(context.getPcefInstance().getProfile().getUserValue());
            if (dbObject != null) {
                if (!context.isWaitForProcess()) { // not wait process --> is new resource --> sent start by resource
                    AFLog.d("State is Usage Monitoring First usage by resource");
                    setState(EState.W_USAGE_MONITORING_UPDATE);
                    nextState = EState.END;
                } else {
                    if (dbConnect.getTransactionService().findMyTransactionDone()) {
                        AFLog.d("My Transaction Done ,Start by other process");
                        dbConnect.getProfileService().updateProfileUnLock(dbConnect.getQuotaService().isHaveNewQuota(), dbConnect.getQuotaService().getMinExpireDate());
                        setResponseSuccess();
                    } else {
                        nextState = EState.CHECK_QUOTA_AVAILIABLE;
                    }
                }
            } else {
                context.setLockByMyTransaction(true);
                context.setWaitForProcess(true);
                AFLog.d("waitForProcess:" + context.isWaitForProcess());
                AFLog.d("lockByMyTransaction:" + context.isLockByMyTransaction());
                E11TimoutService e11TimoutService = new E11TimoutService(appInstance);
                e11TimoutService.buildInterval();
                nextState = EState.FIND_AND_MOD_PROFILE_FOR_WAIT_PROCESS;
            }
        } catch (Exception e) {
            setResponseFail();
            nextState = EState.END;
        }
        setWorkState(nextState);
    }

    @MessageRecieved(messageType = EState.FIND_AND_MOD_PROFILE_FOR_UPDATE_QUOTA_EXHAUST)
    public void findProfileForUpdateQuotaExhaust() {
        EState nextState = null;
        try {
            DBObject dbObject = dbConnect.getProfileService().findAndModifyLockProfile(context.getPcefInstance().getTransaction().getUserValue());
            if (dbObject != null) {
                setState(EState.W_USAGE_MONITORING_UPDATE);
                nextState = EState.END;
                AFLog.d("State is Usage Monitoring Usage with exhaust quota");
            } else {
                //interval
                context.setWaitForProcess(true);
                E11TimoutService e11TimoutService = new E11TimoutService(appInstance);
                e11TimoutService.buildInterval();
                nextState = EState.FIND_AND_MOD_PROFILE_FOR_UPDATE_QUOTA_EXHAUST;
            }

        } catch (Exception e) {
            setResponseFail();
            nextState = EState.END;
        }
        setWorkState(nextState);
    }


}
