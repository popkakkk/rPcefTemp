package phoebe.eqx.pcef.states.mongodb;


import com.mongodb.DBObject;
import phoebe.eqx.pcef.core.model.Quota;
import phoebe.eqx.pcef.enums.state.EMongoState;
import phoebe.eqx.pcef.enums.state.EState;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.instance.CommitPart;
import phoebe.eqx.pcef.instance.Config;
import phoebe.eqx.pcef.services.mogodb.MongoDBConnect;
import phoebe.eqx.pcef.states.mongodb.abs.MessageMongoRecieved;
import phoebe.eqx.pcef.states.mongodb.abs.MongoState;
import phoebe.eqx.pcef.utils.Interval;

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
            List<Quota> quotaExpireList = dbConnect.getQuotaService().findQuotaExpire();
            dbConnect.getQuotaService().setQuotaExpireList(quotaExpireList);

            if (quotaExpireList.size() > 0) {
                boolean modProcessing = dbConnect.getQuotaService().findAndModifyLockQuotaExpire();
                if (modProcessing) {
                    nextState = EMongoState.FIND_USAGE_RESOURCE;
                } else {
                    //interval
                    interval.waitInterval();
                    nextState = EMongoState.FIND_QUOTA_EXPIRE;
                }
            } else {
                // set Profile appointmentDate = (now - appointment)
                // no


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
            List<Quota> quotaExpireList = dbConnect.getQuotaService().getQuotaExpireList();
            Map<String, Integer> countUnitMap = dbConnect.getTransactionService().findTransactionDoneGroupByResource(quotaExpireList);

            int sumTransaction = countUnitMap.values().stream().mapToInt(count -> count).sum();
            if (sumTransaction > 0) {
                setUsageMonitoringState(EState.W_USAGE_MONITORING_UPDATE);
            } else {
                int quotaAllSize = quotaExpireList.size();
                int quotaExpireSize = dbConnect.getQuotaService().findAllQuotaByPrivateId().size();

                if (quotaAllSize > quotaExpireSize) {
                    setUsageMonitoringState(EState.W_USAGE_MONITORING_UPDATE);
                } else if (quotaAllSize == quotaExpireSize) {
                    setUsageMonitoringState(EState.W_USAGE_MONITORING_STOP);
                }
            }

            CommitPart commitPart = new CommitPart();
            commitPart.setQuotaExpireList(quotaExpireList);
            commitPart.setCountUnitMap(countUnitMap);
            appInstance.getPcefInstance().setCommitPart(commitPart);


            nextState = EMongoState.END;

        } catch (Exception e) {

        }
        return nextState;
    }


}
