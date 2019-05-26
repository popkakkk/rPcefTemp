package phoebe.eqx.pcef.states.mongodb;


import com.mongodb.DBObject;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class W_MONGODB_PROCESS_E11_VT_TIMEOUT_STATE extends MongoState {


    private Interval interval = new Interval(Config.RETRY_PROCESSING, Config.INTERVAL_PROCESSING);
    private Interval intervalInsertProfile = new Interval(Config.RETRY_PROCESSING, Config.INTERVAL_PROCESSING);
    private Interval intervalMkIsProcessing = new Interval(Config.RETRY_PROCESSING, Config.INTERVAL_PROCESSING);

    public W_MONGODB_PROCESS_E11_VT_TIMEOUT_STATE(AppInstance appInstance, MongoDBConnect dbConnect) {
        super(appInstance, dbConnect);
    }

    @MessageMongoRecieved(messageType = EMongoState.BEGIN)
    public EMongoState checkProfileAppointmentDate() {
        EMongoState nextState = null;
        try {
            if (dbConnect.getProfileService().findProfileItsAppointmentTime()) {
                DBObject dbObject = dbConnect.getProfileService().findAndModifyLockProfile(appInstance.getPcefInstance().getProfile().getUserValue());
                if (dbObject != null) {
                    nextState = EMongoState.FIND_QUOTA_EXPIRE;
                } else {
                    //interval
                    interval.waitInterval();
                    nextState = EMongoState.BEGIN;
                }
            } else {
                // no
                nextState = EMongoState.END;
            }
        } catch (Exception e) {

        }
        return nextState;
    }


    @MessageMongoRecieved(messageType = EMongoState.FIND_QUOTA_EXPIRE)
    public EMongoState findQuotaExpire() {
        EMongoState nextState = null;
        try {
            List<CommitData> commitDataList = dbConnect.getQuotaService().findDataToCommit(appInstance.getPcefInstance().getProfile().getUserValue(), null, true);
            appInstance.getPcefInstance().setCommitDatas(commitDataList);

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
                    interval.waitInterval();
                    nextState = EMongoState.FIND_QUOTA_EXPIRE;
                }
            } else {
                //End
                setPcefState(EState.END);
                nextState = EMongoState.END;
            }

        } catch (Exception e) {

        }
        return nextState;
    }


    @MessageMongoRecieved(messageType = EMongoState.FIND_USAGE_RESOURCE)
    public EMongoState findUsageResource() {
        EMongoState nextState = null;
        try {
            List<CommitData> commitDataList = appInstance.getPcefInstance().getCommitDatas();

            int sumTransaction = commitDataList.stream().mapToInt(CommitData::getCount).sum();
            if (sumTransaction > 0) {
                setPcefState(EState.W_USAGE_MONITORING_UPDATE);
            } else {
                int quotaExpireSize = appInstance.getPcefInstance().getQuotaCommitSize();
                int quotaAllSize = dbConnect.getQuotaService().findAllQuotaByPrivateId(appInstance.getPcefInstance().getProfile().getUserValue()).size();

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
