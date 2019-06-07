package phoebe.eqx.pcef.states.L2;


import com.mongodb.DBObject;
import phoebe.eqx.pcef.core.model.Quota;
import phoebe.eqx.pcef.enums.state.EState;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.services.E11TimoutService;
import phoebe.eqx.pcef.services.mogodb.MongoDBConnect;
import phoebe.eqx.pcef.states.abs.MessageRecieved;
import phoebe.eqx.pcef.states.abs.MongoState;

import java.util.ArrayList;
import java.util.List;

public class W_MOD_PROCESS extends MongoState {


    public W_MOD_PROCESS(AppInstance appInstance, MongoDBConnect mongoDBConnect) {
        super(appInstance, Level.L2, mongoDBConnect);
    }


    @MessageRecieved(messageType = EState.BEGIN)
    public void wModQuota() {
        EState nextState = null;
        try {

            if (context.getPcefInstance().getQuotaModifyList().size() == 0) {
                List<Quota> quotaList = dbConnect.getQuotaService().getQuotaForModify(context.getPcefInstance().getCommitDataNewList());
                context.getPcefInstance().setQuotaModifyList(quotaList);
            }

            boolean modProcessing = dbConnect.getQuotaService().findAndModifyLockQuotaList(context.getPcefInstance().getQuotaModifyList());
            if (modProcessing) {
                context.getPcefInstance().setCommitDatas(context.getPcefInstance().getCommitDataNewList());
                nextState = EState.FIND_AND_MOD_PROFILE_FOR_WAIT_PROCESS;
            } else {
                //interval
                E11TimoutService e11TimoutService = new E11TimoutService(appInstance);
                e11TimoutService.buildInterval();
                nextState = EState.BEGIN;
            }


        } catch (Exception e) {
            setResponseFail();
            nextState = EState.END;
        }
        setWorkState(nextState);
    }

    @MessageRecieved(messageType = EState.FIND_AND_MOD_PROFILE_FOR_WAIT_PROCESS)
    public void wModProfile() {
        EState nextState = null;
        try {
            DBObject dbObject = dbConnect.getProfileService().findAndModifyLockProfile(context.getPcefInstance().getTransaction().getUserValue());
            if (dbObject != null) {
                setState(EState.W_USAGE_MONITORING_UPDATE);
                nextState = EState.END;
            } else {
                //interval
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


}
