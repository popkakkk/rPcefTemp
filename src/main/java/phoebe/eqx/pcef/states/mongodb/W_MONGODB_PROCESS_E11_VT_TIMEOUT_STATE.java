package phoebe.eqx.pcef.states.mongodb;


import com.mongodb.DBObject;
import phoebe.eqx.pcef.enums.state.EMongoState;
import phoebe.eqx.pcef.enums.state.EState;
import phoebe.eqx.pcef.instance.Config;
import phoebe.eqx.pcef.services.mogodb.MongoDBService;
import phoebe.eqx.pcef.states.mongodb.abs.MessageMongoRecieved;
import phoebe.eqx.pcef.states.mongodb.abs.MongoState;
import phoebe.eqx.pcef.utils.Interval;

public class W_MONGODB_PROCESS_E11_VT_TIMEOUT_STATE extends MongoState {


    public W_MONGODB_PROCESS_E11_VT_TIMEOUT_STATE(MongoDBService mongoDBService) {
        this.mongoDBService = mongoDBService;
    }

    private MongoDBService mongoDBService;
    private EState usageMonitoringState;
    private boolean responseSuccess;


    private Interval interval = new Interval(Config.RETRY_PROCESSING, Config.INTERVAL_PROCESSING);
    private Interval intervalInsertProfile = new Interval(Config.RETRY_PROCESSING, Config.INTERVAL_PROCESSING);
    private Interval intervalMkIsProcessing = new Interval(Config.RETRY_PROCESSING, Config.INTERVAL_PROCESSING);

    @MessageMongoRecieved(messageType = EMongoState.BEGIN)
    public EMongoState checkProfileAppoinmentDate() {
        EMongoState nextState = null;
        try {
            if (mongoDBService.findProfileTimeForAppointmentDate()) {
                DBObject dbObject = mongoDBService.findAndModifyLockProfile();
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
            if (mongoDBService.findQuotaExpire()) {
                DBObject dbObject = mongoDBService.findAndModifyLockQuotaExpire();
                if (dbObject != null) {
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
            if (mongoDBService.findTransactionDoneGroupByResourceQuotaExpire().size() > 0) {
                setUsageMonitoringState(EState.W_USAGE_MONITORING_UPDATE);
                nextState = EMongoState.END;
            } else {

                //count size of quota

                //
                setUsageMonitoringState(EState.W_USAGE_MONITORING_STOP);
                nextState = EMongoState.END;
            }


        } catch (Exception e) {

        }
        return nextState;
    }


}
