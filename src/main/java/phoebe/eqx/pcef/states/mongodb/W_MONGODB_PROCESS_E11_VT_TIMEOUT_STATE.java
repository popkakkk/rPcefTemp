package phoebe.eqx.pcef.states.mongodb;


import com.mongodb.DBObject;
import phoebe.eqx.pcef.enums.state.EMongoState;
import phoebe.eqx.pcef.enums.state.EState;
import phoebe.eqx.pcef.instance.Config;
import phoebe.eqx.pcef.services.mogodb.MongoDBConnect;
import phoebe.eqx.pcef.services.mogodb.MongoDBService;
import phoebe.eqx.pcef.states.mongodb.abs.MessageMongoRecieved;
import phoebe.eqx.pcef.states.mongodb.abs.MongoState;
import phoebe.eqx.pcef.utils.Interval;

public class W_MONGODB_PROCESS_E11_VT_TIMEOUT_STATE extends MongoState {


    public W_MONGODB_PROCESS_E11_VT_TIMEOUT_STATE(MongoDBConnect dbConnect) {
        this.dbConnect = dbConnect;
    }

    private MongoDBConnect dbConnect;
    private EState usageMonitoringState;


    private Interval interval = new Interval(Config.RETRY_PROCESSING, Config.INTERVAL_PROCESSING);
    private Interval intervalInsertProfile = new Interval(Config.RETRY_PROCESSING, Config.INTERVAL_PROCESSING);
    private Interval intervalMkIsProcessing = new Interval(Config.RETRY_PROCESSING, Config.INTERVAL_PROCESSING);

    @MessageMongoRecieved(messageType = EMongoState.BEGIN)
    public EMongoState checkProfileAppointmentDate() {
        EMongoState nextState = null;
        try {
            if (dbConnect.getProfileService().findProfileTimeForAppointmentDate()) {
                DBObject dbObject = dbConnect.getProfileService().findAndModifyLockProfile();
                if (dbObject != null) {
                    nextState = EMongoState.FIND_QUOTA_EXPIRE;
                } else {
                    //interval
                    interval.waitInterval();
                    nextState = EMongoState.BEGIN;
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


    @MessageMongoRecieved(messageType = EMongoState.FIND_QUOTA_EXPIRE)
    public EMongoState findQuotaExpire() {
        EMongoState nextState = null;
        try {
            if (dbConnect.getQuotaService().findQuotaExpire().size() > 0) {
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
            if (dbConnect.getTransactionService().findTransactionDoneGroupByResourceQuotaExpire().size() > 0) {
                setUsageMonitoringState(EState.W_USAGE_MONITORING_UPDATE);
            } else {
                int quotaAllSize = dbConnect.getQuotaService().getQuotaExpireList().size();
                int quotaExpireSize = dbConnect.getQuotaService().findAllQuotaByPrivateId().size();

                if (quotaAllSize > quotaExpireSize) {
                    setUsageMonitoringState(EState.W_USAGE_MONITORING_UPDATE);
                } else if (quotaAllSize == quotaExpireSize) {
                    setUsageMonitoringState(EState.W_USAGE_MONITORING_STOP);
                }
            }
            nextState = EMongoState.END;

        } catch (Exception e) {

        }
        return nextState;
    }


}
