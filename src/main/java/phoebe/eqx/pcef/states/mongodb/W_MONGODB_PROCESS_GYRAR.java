package phoebe.eqx.pcef.states.mongodb;


import com.mongodb.DBObject;
import phoebe.eqx.pcef.enums.state.EMongoState;
import phoebe.eqx.pcef.enums.state.EState;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.instance.Config;
import phoebe.eqx.pcef.message.parser.req.GyRARRequest;
import phoebe.eqx.pcef.services.mogodb.MongoDBConnect;
import phoebe.eqx.pcef.states.mongodb.abs.MessageMongoRecieved;
import phoebe.eqx.pcef.states.mongodb.abs.MongoState;
import phoebe.eqx.pcef.utils.Interval;

public class W_MONGODB_PROCESS_GYRAR extends MongoState {


    private Interval interval = new Interval(Config.RETRY_PROCESSING, Config.INTERVAL_PROCESSING);


    public W_MONGODB_PROCESS_GYRAR(AppInstance appInstance, MongoDBConnect dbConnect) {
        super(appInstance, dbConnect);
    }

    @MessageMongoRecieved(messageType = EMongoState.BEGIN)
    public EMongoState findQuota() {

        try {
            GyRARRequest gyRARRequest = appInstance.getPcefInstance().getGyRARRequest();
            dbConnect.getQuotaService().findAllQuotaByPrivateId(gyRARRequest.getUserValue());

            dbConnect.getQuotaService().findDataToCommit(gyRARRequest.getUserValue(), null, false);

        } catch (Exception e) {

        }
        return EMongoState.FIND_AND_MOD_PROFILE_FOR_WAIT_PROCESS;
    }

    @MessageMongoRecieved(messageType = EMongoState.FIND_AND_MOD_PROFILE_FOR_WAIT_PROCESS)
    public EMongoState findAndModifyProfile() {
        EMongoState nextState = null;
        try {

            DBObject dbObject = dbConnect.getProfileService().findAndModifyLockProfile(appInstance.getPcefInstance().getGyRARRequest().getUserValue());
            if (dbObject != null) {
                setPcefState(EState.W_USAGE_MONITORING_UPDATE);
                nextState = EMongoState.END;
            } else {
                interval.waitInterval();
                nextState = EMongoState.FIND_AND_MOD_PROFILE_FOR_WAIT_PROCESS;
            }

        } catch (Exception e) {

        }
        return nextState;
    }
}
