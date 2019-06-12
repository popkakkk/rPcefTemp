package phoebe.eqx.pcef.services.mogodb;

import com.mongodb.*;
import ec02.af.utils.AFLog;
import phoebe.eqx.pcef.core.logs.summary.SummaryLog;
import phoebe.eqx.pcef.core.model.Profile;
import phoebe.eqx.pcef.enums.DBOperation;
import phoebe.eqx.pcef.enums.EError;
import phoebe.eqx.pcef.enums.Operation;
import phoebe.eqx.pcef.enums.model.EProfile;
import phoebe.eqx.pcef.enums.stats.EStatCmd;
import phoebe.eqx.pcef.enums.stats.EStatMode;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.instance.Config;
import phoebe.eqx.pcef.message.parser.req.UsageMonitoringRequest;
import phoebe.eqx.pcef.utils.DBResult;
import phoebe.eqx.pcef.utils.PCEFUtils;

import java.util.Date;

public class ProfileService extends MongoDBService {


    public ProfileService(AppInstance appInstance, MongoClient mongoClient) {
        super(appInstance, mongoClient, Config.COLLECTION_PROFILE_NAME);
    }

    public void insertProfile() {
        BasicDBObject profileBasicObject = null;
        SummaryLog summaryLog;

        //request
        try {
            UsageMonitoringRequest usageMonitoringRequest = context.getPcefInstance().getUsageMonitoringRequest();
            Profile profile = new Profile();
            profile.set_id(usageMonitoringRequest.getUserValue());
            profile.setUserType("privateId");
            profile.setUserValue(usageMonitoringRequest.getUserValue());
            profile.setIsProcessing(1);
            profile.setSequenceNumber(0);
            profile.setAppointmentDate(new Date());
            profile.setSequenceNumber(0);
            context.getPcefInstance().setProfile(profile);

            profileBasicObject = BasicDBObject.parse(gson.toJson(profile));
            profileBasicObject.put(EProfile.appointmentDate.name(), profile.getAppointmentDate());

            summaryLog = new SummaryLog(Operation.InsertProfile.name(), new Date(), profileBasicObject);
            context.getSummaryLogs().add(summaryLog);

            PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.sent_insert_Profile_request);
            PCEFUtils.writeDBMessageRequest(collectionName, DBOperation.INSERT, profileBasicObject);
        } catch (Exception e) {
            context.setPcefException(PCEFUtils.getPCEFException(e, EError.INSERT_PROFILE_BUILD_ERROR));
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.ERROR, EStatCmd.sent_insert_Profile_request);
            throw e;
        }


        //response
        try {
            WriteResult writeResult = insertByQuery(profileBasicObject);

            BasicDBObject dbResLog = new BasicDBObject();
            dbResLog.put("_id", profileBasicObject.get("_id"));
            PCEFUtils.writeDBMessageResponse(DBResult.SUCCESS, writeResult.getN(), PCEFUtils.getList(dbResLog));

            summaryLog.getSummaryLogDetail().setResponse(new Date(), dbResLog);
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.receive_insert_Profile_response);
        } catch (DuplicateKeyException e) {
            PCEFUtils.writeDBMessageResponseError();
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.DUPPLICATE_KEY, EStatCmd.receive_insert_Profile_response);
            throw e;
        } catch (MongoTimeoutException e) {
            PCEFUtils.writeDBMessageResponseError();
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.TIMEOUT, EStatCmd.receive_insert_Profile_response);
            throw e;
        } catch (MongoException e) {
            PCEFUtils.writeDBMessageResponseError();
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.ERROR, EStatCmd.receive_insert_Profile_response);
            throw e;
        } catch (Exception e) {
            PCEFUtils.writeDBMessageResponseError();
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.ERROR, EStatCmd.receive_insert_Profile_response);
            context.setPcefException(PCEFUtils.getPCEFException(e, EError.INSERT_PROFILE_RESPONSE_ERROR));
            throw e;
        }
    }


    public DBCursor findProfileByPrivateId(String privateId) {

        BasicDBObject searchQuery;
        SummaryLog summaryLog;

        //request
        try {
            searchQuery = new BasicDBObject();
            searchQuery.put(EProfile._id.name(), privateId);

            summaryLog = new SummaryLog(Operation.ReadProfile.name(), new Date(), searchQuery);
            context.getSummaryLogs().add(summaryLog);

            PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.sent_read_Profile_request);
            PCEFUtils.writeDBMessageRequest(collectionName, DBOperation.READ, searchQuery);
        } catch (Exception e) {
            context.setPcefException(PCEFUtils.getPCEFException(e, EError.READ_PROFILE_BUILD_REQUEST_ERROR));
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.ERROR, EStatCmd.sent_read_Profile_request);
            throw e;
        }

        //response
        try {
            DBCursor dbCursor = findByQuery(searchQuery);
            DBObject dbResLog = new BasicDBObject();

            if (dbCursor.hasNext()) {
                DBObject profileDbObject = dbCursor.iterator().next();
                dbResLog = profileDbObject;
                Profile profile = gson.fromJson(gson.toJson(profileDbObject), Profile.class);
                context.getPcefInstance().setProfile(profile);
            }
            PCEFUtils.writeDBMessageResponse(DBResult.SUCCESS, dbCursor.size(), PCEFUtils.getList(dbResLog));

            summaryLog.getSummaryLogDetail().setResponse(new Date(), dbResLog);
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.receive_read_Profile_response);

            return dbCursor;
        } catch (MongoTimeoutException e) {
            PCEFUtils.writeDBMessageResponseError();
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.TIMEOUT, EStatCmd.receive_read_Profile_response);
            context.setPcefException(PCEFUtils.getPCEFException(e, EError.READ_PROFILE_BUILD_REQUEST_ERROR));
            throw e;
        } catch (MongoException e) {
            PCEFUtils.writeDBMessageResponseError();
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.ERROR, EStatCmd.receive_read_Profile_response);
            context.setPcefException(PCEFUtils.getPCEFException(e, EError.READ_PROFILE_BUILD_REQUEST_ERROR));
            throw e;
        }


    }

    private void updateProfileUnlock(BasicDBObject updateQuery) {
        BasicDBObject searchQuery = null;
        SummaryLog summaryLog;
        try {
            AFLog.d("Update Profile Unlock ..");

            updateQuery.put(EProfile.isProcessing.name(), 0);//unlock
            updateQuery.put(EProfile.sequenceNumber.name(), context.getPcefInstance().getProfile().getSequenceNumber());

            searchQuery = new BasicDBObject();
            searchQuery.put(EProfile.userValue.name(), context.getPcefInstance().getProfile().getUserValue());
            searchQuery.put(EProfile.isProcessing.name(), 1);

            summaryLog = new SummaryLog(Operation.UpdateProfile.name(), new Date(), searchQuery.toString() + updateQuery.toString());
            context.getSummaryLogs().add(summaryLog);

            PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.sent_update_Profile_request);
            PCEFUtils.writeDBMessageRequest(collectionName, DBOperation.UPDATE, searchQuery.toString() + updateQuery.toString());
        } catch (Exception e) {
            context.setPcefException(PCEFUtils.getPCEFException(e, EError.UPDATE_PROFILE_REQUEST_ERROR));
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.ERROR, EStatCmd.sent_update_Profile_request);
            throw e;
        }


        try {
            WriteResult writeResult = updateSetByQuery(searchQuery, updateQuery);
            appInstance.getMyContext().setLockProfile(false);

            BasicDBObject dbResLog = new BasicDBObject();
            dbResLog.put("_id", searchQuery.get("_id"));
            PCEFUtils.writeDBMessageResponse(DBResult.SUCCESS, writeResult.getN(), PCEFUtils.getList(dbResLog));

            summaryLog.getSummaryLogDetail().setResponse(new Date(), dbResLog);
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.receive_update_Profile_response);
        } catch (MongoTimeoutException e) {
            PCEFUtils.writeDBMessageResponseError();
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.TIMEOUT, EStatCmd.receive_update_Profile_response);
            context.setPcefException(PCEFUtils.getPCEFException(e, EError.UPDATE_PROFILE_RESPONSE_ERROR));
            throw e;
        } catch (MongoException e) {
            PCEFUtils.writeDBMessageResponseError();
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.ERROR, EStatCmd.receive_update_Profile_response);
            context.setPcefException(PCEFUtils.getPCEFException(e, EError.UPDATE_PROFILE_RESPONSE_ERROR));
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
        SummaryLog summaryLog;
        BasicDBObject query;
        BasicDBObject update;
        try {
            query = new BasicDBObject();
            query.put(EProfile.userValue.name(), privateId);
            query.put(EProfile.isProcessing.name(), 0);

            update = new BasicDBObject();
            update.put(EProfile.isProcessing.name(), 1);//lock

            summaryLog = new SummaryLog(Operation.InsertTransaction.name(), new Date(), query.toString() + update.toString());
            context.getSummaryLogs().add(summaryLog);

            PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.sent_insert_Transaction_request);
            PCEFUtils.writeDBMessageRequest(collectionName, DBOperation.INSERT, query.toString() + update.toString());

        } catch (Exception e) {
            context.setPcefException(PCEFUtils.getPCEFException(e, EError.INSERT_TRANSACTION_BUILD_ERROR));
            PCEFUtils.increaseStatistic(abstractAF, EStatMode.ERROR, EStatCmd.sent_insert_Transaction_request);
            throw e;

        }


        try {


            DBObject dbObject = findAndModify(query, update);
            if (dbObject != null) {
                appInstance.getMyContext().setLockProfile(true);
            }

            return dbObject;
        } catch (Exception e) {
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
