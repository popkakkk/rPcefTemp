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
            UsageMonitoringRequest usageMonitoringRequest = context.getPcefInstance().getUsageMonitoringRequest();
            Profile profile = new Profile();
            profile.set_id(usageMonitoringRequest.getUserValue());
            profile.setUserType("privateId");
            profile.setUserValue(usageMonitoringRequest.getUserValue());
            profile.setIsProcessing(1);

            int firstNumber = 0;
            profile.setSequenceNumber(firstNumber);
            context.getPcefInstance().setProfile(profile);


            insertByObject(profile);
            PCEFUtils.writeMessageFlow("Insert Profile", MessageFlow.Status.Success, context.getPcefInstance().getSessionId());
        } catch (DuplicateKeyException e) {
            PCEFUtils.writeMessageFlow("Insert Profile Duplicate Key", MessageFlow.Status.Error, context.getPcefInstance().getSessionId());
            throw e;
        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Insert Profile", MessageFlow.Status.Error, context.getPcefInstance().getSessionId());
            throw e;
        }
    }


    public boolean checkCanProcessProfile(DBCursor lockProcessCursor) {
        DBObject dbObject = lockProcessCursor.next();
        String isProcessing = String.valueOf(dbObject.get(EProfile.isProcessing.name()));
        return isProcessing.equals("0");
    }


    public DBCursor findProfileByPrivateId(String privateId) {

        try {

            BasicDBObject searchQuery = new BasicDBObject();
            searchQuery.put(EProfile._id.name(), privateId);

            DBCursor dbCursor = findByQuery(searchQuery);

            if (dbCursor.hasNext()) {
                DBObject profileDbObject = dbCursor.iterator().next();

                Profile profile = gson.fromJson(gson.toJson(profileDbObject), Profile.class);
                context.getPcefInstance().setProfile(profile);

                PCEFUtils.writeMessageFlow("Find Profile by privateId [Found]", MessageFlow.Status.Success, context.getPcefInstance().getSessionId());
            } else {
                PCEFUtils.writeMessageFlow("Find Profile by privateId [Not Found]", MessageFlow.Status.Success, context.getPcefInstance().getSessionId());
            }
            return dbCursor;
        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Find Profile by privateId", MessageFlow.Status.Error, context.getPcefInstance().getSessionId());
            throw e;
        }
    }

    private void updateProfileUnlock(BasicDBObject updateQuery) {
        try {
            AFLog.d("Update Profile Unlock ..");

            updateQuery.put(EProfile.isProcessing.name(), 0);//unlock
            updateQuery.put(EProfile.sequenceNumber.name(), context.getPcefInstance().getProfile().getSequenceNumber());

            BasicDBObject searchQuery = new BasicDBObject();
            searchQuery.put(EProfile.userValue.name(), context.getPcefInstance().getProfile().getUserValue());
            searchQuery.put(EProfile.isProcessing.name(), 1);

            updateSetByQuery(searchQuery, updateQuery);
            appInstance.getMyContext().setLockProfile(false);
            PCEFUtils.writeMessageFlow("Update Profile Unlock", MessageFlow.Status.Success, context.getPcefInstance().getSessionId());
        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Update Profile Unlock error-" + e.getStackTrace()[0], MessageFlow.Status.Error, context.getPcefInstance().getSessionId());
            throw e;
        }
    }


    public void updateProfileUnLockInitial(Date quotaMinExpireDate) throws Exception {
        BasicDBObject updateQuery = new BasicDBObject();

        if (quotaMinExpireDate == null) {
            throw new Exception("appointment date null");
        }

        updateQuery.put(EProfile.appointmentDate.name(), quotaMinExpireDate);
        context.getPcefInstance().getProfile().setAppointmentDate(quotaMinExpireDate);
        updateQuery.put(EProfile.sessionId.name(), context.getPcefInstance().getProfile().getSessionId());
        updateProfileUnlock(updateQuery);
    }


    public void updateProfileUnLock(boolean haveNewQuota, Date minExpireDate) {
        BasicDBObject updateQuery = new BasicDBObject();
        if (haveNewQuota) {
            updateQuery.put(EProfile.appointmentDate.name(), minExpireDate);
            context.getPcefInstance().getProfile().setAppointmentDate(minExpireDate);
        }
        updateProfileUnlock(updateQuery);
    }

    public DBObject findAndModifyLockProfile(String privateId) {
        try {
            BasicDBObject query = new BasicDBObject();
            query.put(EProfile.userValue.name(), privateId);
            query.put(EProfile.isProcessing.name(), 0);

            BasicDBObject update = new BasicDBObject();
            update.put(EProfile.isProcessing.name(), 1);//lock

            DBObject dbObject = findAndModify(query, update);

            if (dbObject != null) {
                appInstance.getMyContext().setLockProfile(true);
                PCEFUtils.writeMessageFlow("Find and Modify Profile Lock privateId:" + privateId + ",[Found]", MessageFlow.Status.Success, context.getPcefInstance().getSessionId());
            } else {
                PCEFUtils.writeMessageFlow("Find and Modify Profile Lock privateId:" + privateId + ",[Not Found]", MessageFlow.Status.Success, context.getPcefInstance().getSessionId());
            }

            return dbObject;
        } catch (Exception e) {
            PCEFUtils.writeMessageFlow("Find and Modify Profile Lock privateId:" + privateId + " error" + e.getStackTrace()[0], MessageFlow.Status.Error, context.getPcefInstance().getSessionId());
            throw e;
        }
    }

    public boolean findProfileItsAppointmentTime() {
        Date currentDate = context.getPcefInstance().getStartTime();
        BasicDBObject search = new BasicDBObject();
        search.put(EProfile.appointmentDate.name(), new BasicDBObject("$lte", currentDate));

        DBCursor dbCursor = findByQuery(search);
        if (dbCursor.hasNext()) {//expire
            DBObject dbObject = dbCursor.next();
            Profile profile = gson.fromJson(gson.toJson(dbObject), Profile.class);
            context.getPcefInstance().setProfile(profile);

            AFLog.d("Its time appointment!!");
            AFLog.d("userValue :" + profile.getUserValue());
            AFLog.d("appointmentDate :" + PCEFUtils.isoDateFormatter.format(profile.getAppointmentDate()));
            return true;
        } else {

            AFLog.d("Not Found Profile ,Its not Appointment Time!");

            DBCursor c = db.getCollection(collectionName).find();
            AFLog.d("Find All Profile...");
            while (c.hasNext()) {
                AFLog.d(c.next().toString());
            }


        }

        return false;
    }

    public void removeProfile(String privateId) {
        AFLog.d("Delete Profile privateId:" + privateId + " ..");
        BasicDBObject delete = new BasicDBObject(EProfile._id.name(), privateId);
        writeQueryLog("remove", collectionName, delete.toString());
        db.getCollection(collectionName).remove(delete);
    }
}
