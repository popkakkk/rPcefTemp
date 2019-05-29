package phoebe.eqx.pcef.states.mongodb;


import com.mongodb.DBObject;
import ec02.af.utils.AFLog;
import phoebe.eqx.pcef.core.exceptions.TimeoutIntervalException;
import phoebe.eqx.pcef.core.model.Quota;
import phoebe.eqx.pcef.enums.state.EMongoState;
import phoebe.eqx.pcef.enums.state.EState;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.instance.CommitData;
import phoebe.eqx.pcef.instance.Config;
import phoebe.eqx.pcef.services.mogodb.MongoDBConnect;
import phoebe.eqx.pcef.states.mongodb.abs.MessageMongoRecieved;
import phoebe.eqx.pcef.states.mongodb.abs.MongoState;
import phoebe.eqx.pcef.utils.Interval;
import phoebe.eqx.pcef.utils.PCEFUtils;

import java.util.ArrayList;
import java.util.List;

public class W_MONGODB_PROCESS_E11_VT_TIMEOUT_STATE extends MongoState {

    private Interval intervalFindAndModProfile = new Interval(Config.RETRY_PROCESSING, Config.INTERVAL_PROCESSING);
    private Interval intervalFindAndModQuota = new Interval(Config.RETRY_PROCESSING, Config.INTERVAL_PROCESSING);

    public W_MONGODB_PROCESS_E11_VT_TIMEOUT_STATE(AppInstance appInstance, MongoDBConnect dbConnect) {
        super(appInstance, dbConnect);
    }

    @MessageMongoRecieved(messageType = EMongoState.BEGIN)
    public EMongoState checkProfileAppointmentDate() {
        EMongoState nextState = null;
        try {
            AFLog.d("Find Appointment Date");
            AFLog.d("Current Date:" + PCEFUtils.isoDateFormatter.format(context.getPcefInstance().getStartTime()));

            if (dbConnect.getProfileService().findProfileItsAppointmentTime()) {
                DBObject dbObject = dbConnect.getProfileService().findAndModifyLockProfile(context.getPcefInstance().getProfile().getUserValue());
                if (dbObject != null) {
                    nextState = EMongoState.FIND_QUOTA_EXPIRE;
                } else {
                    //interval
                    intervalFindAndModProfile.waitInterval();
                    nextState = EMongoState.BEGIN;
                }
            } else {
                setPcefState(EState.END);
                nextState = EMongoState.END;
            }
        } catch (TimeoutIntervalException e) {
            setPcefState(EState.END);
            nextState = EMongoState.END;
        } catch (Exception e) {

        }
        return nextState;
    }


    @MessageMongoRecieved(messageType = EMongoState.FIND_QUOTA_EXPIRE)
    public EMongoState findQuotaExpire() {
        EMongoState nextState = null;
        try {
            AFLog.d("Find Quota Expire");
            AFLog.d("Current Date:" + PCEFUtils.isoDateFormatter.format(context.getPcefInstance().getStartTime()));

            List<CommitData> commitDataList = dbConnect.getQuotaService().findDataToCommit(context.getPcefInstance().getProfile().getUserValue(), null, true);
            context.getPcefInstance().setCommitDatas(commitDataList);

            if (commitDataList.size() > 0) {

                List<String> mkCommits = dbConnect.getQuotaService().getMkFromCommitData(commitDataList);
                List<Quota> quotaCommits = new ArrayList<>();
                mkCommits.forEach(s -> {
                    Quota quota = new Quota();
                    quota.setMonitoringKey(s);
                    quota.setProcessing(0);
                    quotaCommits.add(quota);
                });


                boolean modProcessing = dbConnect.getQuotaService().findAndModifyLockQuotaExpire(quotaCommits);
                if (modProcessing) {
                    nextState = EMongoState.FIND_USAGE_RESOURCE;
                } else {
                    //interval
                    intervalFindAndModQuota.waitInterval();
                    nextState = EMongoState.FIND_QUOTA_EXPIRE;
                }
            } else {
                setPcefState(EState.END);
                nextState = EMongoState.END;
            }

        } catch (TimeoutIntervalException e) {
            setPcefState(EState.END);
            nextState = EMongoState.END;
        } catch (Exception e) {

        }
        return nextState;
    }


    @MessageMongoRecieved(messageType = EMongoState.FIND_USAGE_RESOURCE)
    public EMongoState findUsageResource() {
        EMongoState nextState = null;
        try {
            List<CommitData> commitDataList = context.getPcefInstance().getCommitDatas();

            int sumTransaction = commitDataList.stream().mapToInt(CommitData::getCount).sum();
            if (sumTransaction > 0) {
                setPcefState(EState.W_USAGE_MONITORING_UPDATE);
            } else {
                int quotaExpireSize = context.getPcefInstance().getQuotaCommitSize();
                int quotaAllSize = dbConnect.getQuotaService().findAllQuotaByPrivateId(context.getPcefInstance().getProfile().getUserValue()).size();
                AFLog.d("Quota expire size:" + quotaExpireSize + ",All Quota size:" + quotaAllSize);

                if (quotaAllSize > quotaExpireSize) {
                    setPcefState(EState.W_USAGE_MONITORING_UPDATE);
                } else if (quotaAllSize == quotaExpireSize) {
                    setPcefState(EState.W_USAGE_MONITORING_STOP);
                }
            }
            nextState = EMongoState.END;

        } catch (Exception e) {

        }
        return nextState;
    }


}
