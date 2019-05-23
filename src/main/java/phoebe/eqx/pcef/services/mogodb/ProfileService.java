package phoebe.eqx.pcef.services.mogodb;

import com.mongodb.*;
import ec02.af.utils.AFLog;
import phoebe.eqx.pcef.core.model.Profile;
import phoebe.eqx.pcef.enums.model.EProfile;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.instance.Config;
import phoebe.eqx.pcef.message.parser.req.UsageMonitoringRequest;
import phoebe.eqx.pcef.utils.MessageFlow;
import phoebe.eqx.pcef.utils.PCEFUtils;

import java.util.Date;

public class ProfileService extends MongoDBService {


    public ProfileService(AppInstance appInstance, MongoClient mongoClient) {
        super(appInstance, mongoClient, Config.COLLECTION_PROFILE_NAME);
    }

    public void insertProfile() {
        try {
            UsageMonitoringRequest usageMonitoringRequest = appInstance.getPcefInstance().getUsageMonitoringRequest();
            Profile profile = new Profile();
            profile.set_id(usageMonitoringRequest.getUserValue());
            profile.setUserType("privateId");
            profile.setUserValue(usageMonitoringRequest.getUserValue());
            profile.setIsProcessing(1);

            int firstNumber = 0;
            profile.setSequenceNumber(firstNumber);
            appInstance.getPcefInstance().setProfile(profile);


            insertByObject(profile);
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
            String privateId = appInstance.getPcefInstance().getUsageMonitoringRequest().getUserValue();
            BasicDBObject searchQuery = new BasicDBObject();
            searchQuery.put(EProfile._id.name(), privateId);

            DBCursor dbCursor = findByQuery(searchQuery);


            if (dbCursor.hasNext()) {
                DBObject profileDbObject = dbCursor.iterator().next();

                Profile profile = gson.fromJson(gson.toJson(profileDbObject), Profile.class);
                appInstance.getPcefInstance().setProfile(profile);

                /*Date appointmentDate = (Date) profileDbObject.get(EProfile.appointmentDate.name());
                String sessionId = (String) profileDbObject.get(EProfile.sessionId.name());
                Integer sequenceNumber = (Integer) profileDbObject.get(EProfile.sequenceNumber.name());

                appInstance.getPcefInstance().setAppointmentDate(appointmentDate);
                appInstance.getPcefInstance().setOcfSessionId(sessionId);
                appInstance.getPcefInstance().setSequenceNumber(sequenceNumber);*/

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
        updateQuery.put(EProfile.sequenceNumber.name(), appInstance.getPcefInstance().getProfile().getSequenceNumber());

        BasicDBObject searchQuery = new BasicDBObject();
        searchQuery.put(EProfile.userValue.name(), appInstance.getPcefInstance().getProfile().getUserValue());
        searchQuery.put(EProfile.isProcessing.name(), 1);

        updateSetByQuery(searchQuery, updateQuery);
    }


    public void updateProfileUnLockInitial(Date quotaMinExpireDate) throws Exception {
        BasicDBObject updateQuery = new BasicDBObject();

        if (quotaMinExpireDate == null) {
            throw new Exception("appointment date null");
        }

        updateQuery.put(EProfile.appointmentDate.name(), quotaMinExpireDate);
        appInstance.getPcefInstance().getProfile().setAppointmentDate(quotaMinExpireDate);
        updateQuery.put(EProfile.sessionId.name(), appInstance.getPcefInstance().getProfile().getSessionId());
        updateProfileUnlock(updateQuery);
    }


    public void updateProfileUnLock(boolean haveNewQuota, Date minExpireDate) {
        BasicDBObject updateQuery = new BasicDBObject();
        if (haveNewQuota) {
            if (minExpireDate.before(appInstance.getPcefInstance().getProfile().getAppointmentDate())) {
                updateQuery.put(EProfile.appointmentDate.name(), minExpireDate);
                appInstance.getPcefInstance().getProfile().setAppointmentDate(minExpireDate);
            }
        }
        updateProfileUnlock(updateQuery);
    }

    public DBObject findAndModifyLockProfile(String privateId) {
        BasicDBObject query = new BasicDBObject();
        query.put(EProfile.userValue.name(), privateId);
        query.put(EProfile.isProcessing.name(), 0);

        BasicDBObject update = new BasicDBObject();
        update.put(EProfile.isProcessing.name(), 1);//lock

        return findAndModify(query, update);
    }

    public boolean findProfileItsAppointmentTime() {
        Date currentTime = appInstance.getPcefInstance().getStartTime();
        BasicDBObject search = new BasicDBObject();
        search.put(EProfile.appointmentDate.name(), new BasicDBObject("$lte", currentTime));

        DBCursor dbCursor = findByQuery(search);
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

    public void removeProfile(String privateId) {
        BasicDBObject delete = new BasicDBObject(EProfile._id.name(), privateId);
        db.getCollection(Config.COLLECTION_TRANSACTION_NAME).remove(delete);
    }
}
