package phoebe.eqx.pcef.services.mogodb;

import com.mongodb.*;
import ec02.af.utils.AFLog;
import phoebe.eqx.pcef.core.model.Profile;
import phoebe.eqx.pcef.enums.config.EConfig;
import phoebe.eqx.pcef.enums.model.EProfile;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.instance.Config;
import phoebe.eqx.pcef.message.parser.req.UsageMonitoringRequest;
import phoebe.eqx.pcef.utils.MessageFlow;
import phoebe.eqx.pcef.utils.PCEFUtils;

import java.util.Date;

public class ProfileService extends MongoDBService {

    private Date minExpireDate;
    private boolean haveNewQuota;



    public ProfileService(AppInstance appInstance, MongoClient mongoClient) {
        super(appInstance, mongoClient);
    }

    public void insertProfile() {
        try {
            UsageMonitoringRequest usageMonitoringRequest = appInstance.getPcefInstance().getUsageMonitoringRequest();
            Profile profile = new Profile();
            profile.set_id(usageMonitoringRequest.getPrivateId());
            profile.setUserType("privateId");
            profile.setUserValue(usageMonitoringRequest.getPrivateId());
            profile.setIsProcessing(1);

            int firstNumber = 0;
            profile.setSequenceNumber(firstNumber);
            appInstance.getPcefInstance().setSequenceNumber(firstNumber);

            insertByObject(Config.COLLECTION_PROFILE_NAME, profile);
            PCEFUtils.writeMessageFlow("Insert Profile", MessageFlow.Status.Success, appInstance.getPcefInstance().getSessionId());
        } catch (DuplicateKeyException e) {
            PCEFUtils.writeMessageFlow("Insert Profile Duplicate Key", MessageFlow.Status.Error, appInstance.getPcefInstance().getSessionId());
            throw e;
        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Insert Profile", MessageFlow.Status.Error, appInstance.getPcefInstance().getSessionId());
            throw e;
        }
    }


    public boolean checkCanProcessProfile(DBCursor lockProcessCursor) {
        DBObject dbObject = lockProcessCursor.next();
        String isProcessing = String.valueOf(dbObject.get(EProfile.isProcessing.name()));
        return isProcessing.equals("0");
    }


    public DBCursor findProfileByPrivateId() {

        try {
            String privateId = appInstance.getPcefInstance().getUsageMonitoringRequest().getPrivateId();
            BasicDBObject searchQuery = new BasicDBObject();
            searchQuery.put(EProfile._id.name(), privateId);

            DBCursor dbCursor = findByQuery(Config.COLLECTION_PROFILE_NAME, searchQuery);


            if (dbCursor.hasNext()) {
                DBObject profileDbObject = dbCursor.iterator().next();

                Date appointmentDate = (Date) profileDbObject.get(EProfile.appointmentDate.name());
                String sessionId = (String) profileDbObject.get(EProfile.sessionId.name());
                Integer sequenceNumber = (Integer) profileDbObject.get(EProfile.sequenceNumber.name());

                appInstance.getPcefInstance().setAppointmentDate(appointmentDate);
                appInstance.getPcefInstance().setOcfSessionId(sessionId);
                appInstance.getPcefInstance().setSequenceNumber(sequenceNumber);

                PCEFUtils.writeMessageFlow("Find Profile by privateId [Found]", MessageFlow.Status.Success, appInstance.getPcefInstance().getSessionId());
            } else {
                PCEFUtils.writeMessageFlow("Find Profile by privateId [Not Found]", MessageFlow.Status.Success, appInstance.getPcefInstance().getSessionId());
            }
            return dbCursor;
        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Find Profile by privateId", MessageFlow.Status.Error, appInstance.getPcefInstance().getSessionId());
            throw e;
        }
    }

    private void updateProfileUnlock(BasicDBObject updateQuery) {
        updateQuery.put(EProfile.isProcessing.name(), 0);//unlock
        updateQuery.put(EProfile.sequenceNumber.name(), appInstance.getPcefInstance().getSequenceNumber());

        BasicDBObject searchQuery = new BasicDBObject();
        searchQuery.put(EProfile.userValue.name(), appInstance.getPcefInstance().getTransaction().getUserValue());
        searchQuery.put(EProfile.isProcessing.name(), 1);

        updateSetByQuery(Config.COLLECTION_PROFILE_NAME, searchQuery, updateQuery);
    }


    public void updateProfileUnLockInitial() {
        BasicDBObject updateQuery = new BasicDBObject();
        updateQuery.put(EProfile.appointmentDate.name(), minExpireDate);
        updateQuery.put(EProfile.sessionId.name(), appInstance.getPcefInstance().getOcfSessionId());
        updateProfileUnlock(updateQuery);
    }


    public void updateProfileUnLock() {
        BasicDBObject updateQuery = new BasicDBObject();
        if (haveNewQuota) {
            if (minExpireDate.before(appInstance.getPcefInstance().getAppointmentDate())) {
                updateQuery.put(EProfile.appointmentDate.name(), minExpireDate);
                appInstance.getPcefInstance().setAppointmentDate(minExpireDate);
            }
        }
        updateProfileUnlock(updateQuery);
    }

    public DBObject findAndModifyLockProfile() {

        String privateId = appInstance.getPcefInstance().getTransaction().getUserValue();
        BasicDBObject query = new BasicDBObject();
        query.put(EProfile.userValue.name(), privateId);
        query.put(EProfile.isProcessing.name(), 0);

        BasicDBObject update = new BasicDBObject();
        update.put(EProfile.isProcessing.name(), 1);//lock

        return findAndModify(Config.COLLECTION_PROFILE_NAME, query, update);
    }
    public boolean findProfileTimeForAppointmentDate() {


        Date currentTime = appInstance.getPcefInstance().getStartTime();
        BasicDBObject search = new BasicDBObject();
        search.put(EProfile.appointmentDate.name(), new BasicDBObject("$lte", currentTime));

        DBCursor dbCursor = findByQuery(EConfig.COLLECTION_PROFILE_NAME.getConfigName(), search);
        if (dbCursor.hasNext()) {//expire
            DBObject dbObject = dbCursor.next();
            Profile profile = gson.fromJson(gson.toJson(dbObject), Profile.class);
            appInstance.getPcefInstance().setProfile(profile);

            AFLog.d("userValue :" + profile.getUserValue());
            AFLog.d("appointmentDate :" + profile.getAppointmentDate());
            AFLog.d("currentTime = :" + currentTime);

            return true;
        } else {
            AFLog.d("profile not found");
        }

        return false;
    }

}
