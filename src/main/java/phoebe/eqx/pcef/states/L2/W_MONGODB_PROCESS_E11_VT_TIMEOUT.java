package phoebe.eqx.pcef.states.L2;


import com.mongodb.DBObject;
import ec02.af.utils.AFLog;
import phoebe.eqx.pcef.core.exceptions.TimeoutIntervalException;
import phoebe.eqx.pcef.core.model.Quota;
import phoebe.eqx.pcef.enums.state.EState;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.instance.CommitData;
import phoebe.eqx.pcef.services.E11TimoutService;
import phoebe.eqx.pcef.services.mogodb.MongoDBConnect;
import phoebe.eqx.pcef.states.abs.MessageRecieved;
import phoebe.eqx.pcef.states.abs.MongoState;
import phoebe.eqx.pcef.utils.PCEFUtils;

import java.util.List;

public class W_MONGODB_PROCESS_E11_VT_TIMEOUT extends MongoState {


    public W_MONGODB_PROCESS_E11_VT_TIMEOUT(AppInstance appInstance, MongoDBConnect dbConnect) {
        super(appInstance, Level.L2, dbConnect);
    }

    @MessageRecieved(messageType = EState.BEGIN)
    public void checkProfileAppointmentDate() {
        EState nextState = null;
        try {
            AFLog.d("Find Appointment Date");
            AFLog.d("Current Date:" + PCEFUtils.isoDateFormatter.format(context.getPcefInstance().getStartTime()));

            if (dbConnect.getProfileService().findProfileItsAppointmentTime()) {
                DBObject dbObject = dbConnect.getProfileService().findAndModifyLockProfile(context.getPcefInstance().getProfile().getUserValue());
                if (dbObject != null) {
                    nextState = EState.FIND_QUOTA_EXPIRE;
                } else {
                    //interval
                    E11TimoutService e11TimoutService = new E11TimoutService(appInstance);
                    e11TimoutService.buildInterval();

                    nextState = EState.BEGIN;
                }
            } else {
                setResponseFail();
                nextState = EState.END;
            }
        } catch (TimeoutIntervalException e) {
            setResponseFail();
            nextState = EState.END;
        } catch (Exception e) {
            setResponseFail();
            nextState = EState.END;
        }
        setWorkState(nextState);
    }


    @MessageRecieved(messageType = EState.FIND_QUOTA_EXPIRE)
    public void findQuotaExpire() {
        EState nextState = null;
        try {
            AFLog.d("Find Quota Expire");
            AFLog.d("Current Date:" + PCEFUtils.isoDateFormatter.format(context.getPcefInstance().getStartTime()));


            if (context.getPcefInstance().getQuotaModifyList().size() == 0) {
                List<CommitData> commitDataList = dbConnect.getQuotaService().findDataToCommit(context.getPcefInstance().getProfile().getUserValue(), null, true);
                context.getPcefInstance().setCommitDatas(commitDataList);

                List<Quota> quotaList = dbConnect.getQuotaService().getQuotaForModify(commitDataList);
                context.getPcefInstance().setQuotaModifyList(quotaList);
            }


            if (context.getPcefInstance().getCommitDatas().size() > 0) {
                boolean modProcessing = dbConnect.getQuotaService().findAndModifyLockQuotaList(context.getPcefInstance().getQuotaModifyList());
                if (modProcessing) {
                    nextState = EState.FIND_USAGE_RESOURCE;
                } else {
                    //interval
                    E11TimoutService e11TimoutService = new E11TimoutService(appInstance);
                    e11TimoutService.buildInterval();
                    nextState = EState.FIND_QUOTA_EXPIRE;
                }
            } else {
                setResponseFail();
                nextState = EState.END;
            }

        } catch (TimeoutIntervalException e) {
            setResponseFail();
            nextState = EState.END;
        } catch (Exception e) {
            setResponseFail();
            nextState = EState.END;
        }
        setWorkState(nextState);
    }


    @MessageRecieved(messageType = EState.FIND_USAGE_RESOURCE)
    public void findUsageResource() {
        EState nextState = null;
        try {
            List<CommitData> commitDataList = context.getPcefInstance().getCommitDatas();

            int sumTransaction = commitDataList.stream().mapToInt(CommitData::getCount).sum();
            if (sumTransaction > 0) {
                AFLog.d("Found Transaction usage =" + sumTransaction + ",to build Usage Monitoring Update");
                setState(EState.W_USAGE_MONITORING_UPDATE);
            } else {
                int quotaExpireSize = context.getPcefInstance().getQuotaCommitSize();
                int quotaAllSize = dbConnect.getQuotaService().findAllQuotaByPrivateId(context.getPcefInstance().getProfile().getUserValue()).size();
                AFLog.d("Quota expire size:" + quotaExpireSize + ",All Quota size:" + quotaAllSize);

                if (quotaAllSize > quotaExpireSize) {
                    AFLog.d("(Quota expire size) < (All Quota size),to build Usage Monitoring Update");
                    setState(EState.W_USAGE_MONITORING_UPDATE);
                } else if (quotaAllSize == quotaExpireSize) {
                    AFLog.d("(Quota expire size) == (All Quota size),to build Usage Monitoring Stop");
                    setState(EState.W_USAGE_MONITORING_STOP);
                }
            }
            nextState = EState.END;

        } catch (Exception e) {
            setResponseFail();
            nextState = EState.END;
        }
        setWorkState(nextState);
    }


}
