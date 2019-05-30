package phoebe.eqx.pcef.states.mongodb;


import com.google.gson.Gson;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.DuplicateKeyException;
import ec02.af.utils.AFLog;
import phoebe.eqx.pcef.core.exceptions.TimeoutIntervalException;
import phoebe.eqx.pcef.core.model.Quota;
import phoebe.eqx.pcef.core.model.Transaction;
import phoebe.eqx.pcef.enums.state.EMongoState;
import phoebe.eqx.pcef.enums.state.EState;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.instance.CommitData;
import phoebe.eqx.pcef.instance.Config;
import phoebe.eqx.pcef.services.mogodb.MongoDBConnect;
import phoebe.eqx.pcef.states.mongodb.abs.MessageMongoRecieved;
import phoebe.eqx.pcef.states.mongodb.abs.MongoState;
import phoebe.eqx.pcef.utils.Interval;

import java.util.ArrayList;
import java.util.List;

public class W_MONGODB_PROCESS_STATE extends MongoState {


    private boolean responseSuccess;


    //flag
    private boolean waitForProcess;
    private boolean lockByMyTransaction;


    private Interval intervalInsertProfile = new Interval(Config.RETRY_PROCESSING, Config.INTERVAL_PROCESSING);
    private Interval intervalMkIsProcessing = new Interval(Config.RETRY_PROCESSING, Config.INTERVAL_PROCESSING);
    private Interval intervalFindAndModQuota = new Interval(Config.RETRY_PROCESSING, Config.INTERVAL_PROCESSING);
    private Interval intervalFindAndModProfile = new Interval(Config.RETRY_PROCESSING, Config.INTERVAL_PROCESSING);

    public W_MONGODB_PROCESS_STATE(AppInstance appInstance, MongoDBConnect dbConnect) {
        super(appInstance, dbConnect);
    }

    @MessageMongoRecieved(messageType = EMongoState.BEGIN)
    public EMongoState InitialProcess() {
        EMongoState nextState = null;
        try {
            DBCursor profileCursor = dbConnect.getProfileService().findProfileByPrivateId(context.getPcefInstance().getUsageMonitoringRequest().getUserValue());

            if (!profileCursor.hasNext()) {
                nextState = EMongoState.INSERT_PROFILE;
            } else {
                nextState = EMongoState.CHECK_QUOTA_AVAILIABLE;
            }
        } catch (Exception e) {
            setResponseFail();
            nextState = EMongoState.END;
        }
        return nextState;
    }


    @MessageMongoRecieved(messageType = EMongoState.INSERT_PROFILE)
    public EMongoState insertProfileState() {
        EMongoState nextState;
        try {
            dbConnect.getProfileService().insertProfile();
            setPcefState(EState.W_USAGE_MONITORING_START);
            nextState = EMongoState.END;
        } catch (DuplicateKeyException e) {
            nextState = EMongoState.CHECK_QUOTA_AVAILIABLE;
        } catch (Exception e) {
            try {
                intervalInsertProfile.waitInterval();
                nextState = EMongoState.INSERT_PROFILE;
            } catch (TimeoutIntervalException ex) {
                setResponseFail();
                nextState = EMongoState.END;
            }

        }
        return nextState;
    }


    @MessageMongoRecieved(messageType = EMongoState.CHECK_QUOTA_AVAILIABLE)
    public EMongoState checkQuotaAvailable() {
        EMongoState nextState = null;
        waitForProcess = false;
        try {
            DBCursor QuotaCursor = dbConnect.getQuotaService().findQuotaByTransaction(context.getPcefInstance().getTransaction());
            if (!QuotaCursor.hasNext()) {

                if (lockByMyTransaction) {
                    setPcefState(EState.W_USAGE_MONITORING_UPDATE);
                    nextState = EMongoState.END;
                } else {
                    nextState = EMongoState.FIND_AND_MOD_PROFILE_FOR_WAIT_PROCESS;
                }

            } else {
                if (dbConnect.getQuotaService().checkMkCanProcess(QuotaCursor)) {
                    Quota quota = new Gson().fromJson(new Gson().toJson(QuotaCursor.iterator().next()), Quota.class);

                    /* ----------------- [Check Quota Exhaust]------------------------------------*/
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
                        nextState = EMongoState.END;
                    } else {
                        AFLog.d("Quota Exhaust");
                        context.getPcefInstance().setCommitDatas(commitDataList);
                        DBObject findModQuota = dbConnect.getQuotaService().findAndModifyLockQuota(quota.getMonitoringKey());
                        if (findModQuota != null) {
                            nextState = EMongoState.FIND_AND_MOD_PROFILE_FOR_UPDATE_QUOTA_EXHAUST;
                        } else {
                            intervalFindAndModQuota.waitInterval();
                            nextState = EMongoState.CHECK_QUOTA_AVAILIABLE;
                        }
                    }
                } else {
                    intervalMkIsProcessing.waitInterval();
                    nextState = EMongoState.CHECK_QUOTA_AVAILIABLE;
                }
            }
        } catch (Exception e) {
            setResponseFail();
            nextState = EMongoState.END;

        }
        return nextState;
    }

    @MessageMongoRecieved(messageType = EMongoState.FIND_AND_MOD_PROFILE_FOR_WAIT_PROCESS)
    public EMongoState findAndModProfileForWaitProcess() {
        EMongoState nextState = null;
        try {

            DBObject dbObject = dbConnect.getProfileService().findAndModifyLockProfile(context.getPcefInstance().getProfile().getUserValue());
            if (dbObject != null) {
                if (!waitForProcess) { // not wait process --> is new resource --> sent start by resource
                    AFLog.d("State is Usage Monitoring First usage by resource");
                    setPcefState(EState.W_USAGE_MONITORING_UPDATE);
                    nextState = EMongoState.END;
                } else {
                    if (dbConnect.getTransactionService().findMyTransactionDone()) {
                        AFLog.d("My Transaction Done ,Start by other process");
                        dbConnect.getProfileService().updateProfileUnLock(dbConnect.getQuotaService().isHaveNewQuota(), dbConnect.getQuotaService().getMinExpireDate());
                        setResponseSuccess();
                    } else {
                        nextState = EMongoState.CHECK_QUOTA_AVAILIABLE;
                    }
                }
            } else {
                lockByMyTransaction = true;
                waitForProcess = true;
                AFLog.d("waitForProcess:" + waitForProcess);
                AFLog.d("lockByMyTransaction:" + lockByMyTransaction);
                intervalFindAndModProfile.waitInterval();
                nextState = EMongoState.FIND_AND_MOD_PROFILE_FOR_WAIT_PROCESS;
            }


        } catch (Exception e) {
            setResponseFail();
            nextState = EMongoState.END;
        }
        return nextState;
    }

    @MessageMongoRecieved(messageType = EMongoState.FIND_AND_MOD_PROFILE_FOR_UPDATE_QUOTA_EXHAUST)
    public EMongoState findProfileForUpdateQuotaExhaust() {
        EMongoState nextState = null;
        try {
            DBObject dbObject = dbConnect.getProfileService().findAndModifyLockProfile(context.getPcefInstance().getTransaction().getUserValue());
            if (dbObject != null) {
                setPcefState(EState.W_USAGE_MONITORING_UPDATE);
                nextState = EMongoState.END;
                AFLog.d("State is Usage Monitoring Usage with exhaust quota");
            } else {
                //interval
                waitForProcess = true;
                intervalFindAndModProfile.waitInterval();
                nextState = EMongoState.FIND_AND_MOD_PROFILE_FOR_UPDATE_QUOTA_EXHAUST;
            }

        } catch (Exception e) {
            setResponseFail();
            nextState = EMongoState.END;
        }
        return nextState;
    }

    public boolean isResponseSuccess() {
        return responseSuccess;
    }

    public void setResponseSuccess() {
        setPcefState(EState.END);
        this.responseSuccess = true;
    }


    public void setResponseFail() {
        setPcefState(EState.END);
        this.responseSuccess = false;
    }

}
