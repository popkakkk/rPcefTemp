package phoebe.eqx.pcef.states.L2;


import com.mongodb.DBObject;
import phoebe.eqx.pcef.core.model.Profile;
import phoebe.eqx.pcef.enums.state.EState;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.instance.CommitData;
import phoebe.eqx.pcef.message.parser.req.GyRARRequest;
import phoebe.eqx.pcef.services.E11TimoutService;
import phoebe.eqx.pcef.services.mogodb.MongoDBConnect;
import phoebe.eqx.pcef.states.abs.MessageRecieved;
import phoebe.eqx.pcef.states.abs.MongoState;

import java.util.List;

public class W_MONGODB_PROCESS_GYRAR extends MongoState {


    public W_MONGODB_PROCESS_GYRAR(AppInstance appInstance, MongoDBConnect dbConnect) {
        super(appInstance, Level.L2, dbConnect);
    }


   /* @MessageRecieved(messageType = EState.BEGIN)
    public void findQuota() {
        EState nextState = null;

        try {



            nextState = EState.REMOVE_QUOTA_GYRAR;
        } catch (Exception e) {
            setResponseFail();
            nextState = EState.END;
        }
        setWorkState(nextState);
    }
*/
    @MessageRecieved(messageType = EState.BEGIN)
    public void findQuota() {
        EState nextState;
        try {

            DBObject dbObject = dbConnect.getProfileService().findAndModifyLockProfile(context.getPcefInstance().getGyRARRequest().getUserValue());
            if (dbObject != null) {
                Profile profile = gson.fromJson(gson.toJson(dbObject), Profile.class);
                context.getPcefInstance().setProfile(profile);

                //remove quota
                GyRARRequest gyRARRequest = context.getPcefInstance().getGyRARRequest();
                List<CommitData> commitDataList = dbConnect.getQuotaService().findDataToCommit(gyRARRequest.getUserValue(), null, false);
                context.getPcefInstance().setCommitDatas(commitDataList);

                dbConnect.getQuotaService().removeQuota(context.getPcefInstance().getGyRARRequest().getUserValue());

                setState(EState.W_USAGE_MONITORING_UPDATE);
                nextState = EState.END;
            } else {
                E11TimoutService e11TimoutService = new E11TimoutService(appInstance);
                e11TimoutService.buildInterval();
                nextState = EState.REMOVE_QUOTA_GYRAR;
            }

        } catch (Exception e) {
            setResponseFail();
            nextState = EState.END;
        }
        setWorkState(nextState);
    }
}
