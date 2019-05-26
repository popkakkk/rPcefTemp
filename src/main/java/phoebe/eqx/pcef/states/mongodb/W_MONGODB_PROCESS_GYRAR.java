package phoebe.eqx.pcef.states.mongodb;


import com.mongodb.DBObject;
import phoebe.eqx.pcef.core.model.Profile;
import phoebe.eqx.pcef.enums.state.EMongoState;
import phoebe.eqx.pcef.enums.state.EState;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.instance.CommitData;
import phoebe.eqx.pcef.instance.Config;
import phoebe.eqx.pcef.message.parser.req.GyRARRequest;
import phoebe.eqx.pcef.services.mogodb.MongoDBConnect;
import phoebe.eqx.pcef.states.mongodb.abs.MessageMongoRecieved;
import phoebe.eqx.pcef.states.mongodb.abs.MongoState;
import phoebe.eqx.pcef.utils.Interval;

import java.util.List;

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

            List<CommitData> commitDataList =   dbConnect.getQuotaService().findDataToCommit(gyRARRequest.getUserValue(), null, false);
            appInstance.getPcefInstance().setCommitDatas(commitDataList);

        } catch (Exception e) {

        }
        return EMongoState.REMOVE_QUOTA_GYRAR;
    }

    @MessageMongoRecieved(messageType = EMongoState.REMOVE_QUOTA_GYRAR)
    public EMongoState findAndModifyProfile() {
        EMongoState nextState = null;
        try {

            DBObject dbObject = dbConnect.getProfileService().findAndModifyLockProfile(appInstance.getPcefInstance().getGyRARRequest().getUserValue());
            if (dbObject != null) {
                Profile profile = gson.fromJson(gson.toJson(dbObject), Profile.class);
                appInstance.getPcefInstance().setProfile(profile);

                //remove quota
                dbConnect.getQuotaService().removeQuota(appInstance.getPcefInstance().getGyRARRequest().getUserValue());

                setPcefState(EState.W_USAGE_MONITORING_UPDATE);
                nextState = EMongoState.END;
            } else {
                interval.waitInterval();
                nextState = EMongoState.REMOVE_QUOTA_GYRAR;
            }

        } catch (Exception e) {

        }
        return nextState;
    }
}
