package phoebe.eqx.pcef.services.mogodb;

import com.google.gson.Gson;
import com.mongodb.*;
import ec02.af.utils.AFLog;
import phoebe.eqx.pcef.enums.EStatusLifeCycle;
import phoebe.eqx.pcef.enums.model.EQuota;
import phoebe.eqx.pcef.enums.model.ETransaction;
import phoebe.eqx.pcef.enums.model.element.EResourceQuota;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.instance.CommitData;
import phoebe.eqx.pcef.instance.Config;

import java.util.*;

public abstract class MongoDBService {

    protected AppInstance appInstance;
    protected DB db;
    protected String collectionName;

    public Gson gson = new Gson();


    public MongoDBService(AppInstance appInstance, MongoClient mongoClient, String collectionName) {
        this.db = mongoClient.getDB(Config.MY_DB_NAME);
        this.appInstance = appInstance;
        this.collectionName = collectionName;
    }


    public void insertByObject(Object object) {
        String json = gson.toJson(object);
        writeQueryLog("insert", collectionName, json);
        db.getCollection(collectionName).insert(BasicDBObject.parse(json));
    }

    public void insertByQuery(BasicDBObject basicDBObject) {
        db.getCollection(collectionName).insert(basicDBObject);
    }

    public void insertManyByObject(List<BasicDBObject> insertList) {
        writeQueryLog("insert many", collectionName, gson.toJson(insertList));
        db.getCollection(collectionName).insert(insertList);
    }

    public void updatePush(BasicDBObject searchQuery, BasicDBObject updateQuery) {
        BasicDBObject pushUpdate = new BasicDBObject("$push", updateQuery);
        String condition = "searchQuery:" + searchQuery.toJson() + ",updateQuery:" + pushUpdate.toJson();
        writeQueryLog("update", collectionName, condition);
        db.getCollection(collectionName).update(searchQuery, pushUpdate);
    }


    public DBCursor findByObject(Object object) {
        String json = gson.toJson(object);
        writeQueryLog("find", collectionName, json);
        return db.getCollection(collectionName).find(BasicDBObject.parse(json));
    }

    public DBCursor findByQuery(BasicDBObject whereQuery) {
        writeQueryLog("find", collectionName, whereQuery.toJson());
        return db.getCollection(collectionName).find(whereQuery);
    }

    public void updateSetByQuery(BasicDBObject searchQuery, BasicDBObject updateQuery) {
        BasicDBObject setUpdate = new BasicDBObject("$set", updateQuery);
        String condition = "searchQuery:" + searchQuery.toJson() + ",updateQuery:" + setUpdate.toJson();
        writeQueryLog("update", collectionName, condition);
        db.getCollection(collectionName).update(searchQuery, setUpdate);
    }


    public Iterable<DBObject> aggregateMatch(BasicDBObject match, BasicDBObject group) {
        List<DBObject> pipeline = Arrays.asList(
                new BasicDBObject("$match", match)
                , new BasicDBObject("$group", group));

        String condition = "$match:" + match.toJson() + ",$group:" + group.toJson();
        writeQueryLog("aggregate", collectionName, condition);
        return db.getCollection(collectionName).aggregate(pipeline).results();
    }


    public DBObject findAndModify(BasicDBObject query, BasicDBObject update) {
        BasicDBObject setUpdate = new BasicDBObject("$set", update);
        return db.getCollection(collectionName).findAndModify(query, setUpdate);
    }


    public List distinctByObject(String fieldName, Object object) {
        return db.getCollection(collectionName).distinct(fieldName, BasicDBObject.parse(gson.toJson(object)));
    }


    private void writeQueryLog(String action, String collection, String condition) {
        AFLog.d("[Query] " + action + " " + collection + ", condition =" + condition);
    }


    public void findDataToCommit(String privateId, String monitoringKey, boolean quotaExpire) {

        try {

            String tempNameTransaction = "transactionData";
            String tempNameTids = "transactionIds";

            BasicDBObject $lookupTransaction = new BasicDBObject();
            $lookupTransaction.put("from", Config.COLLECTION_TRANSACTION_NAME);
            $lookupTransaction.put("localField", EQuota.resources.name() + "." + EResourceQuota.resourceId.name());
            $lookupTransaction.put("foreignField", ETransaction.resourceId.name());
            $lookupTransaction.put("as", tempNameTransaction);


            BasicDBObject $unwindTransactionData = new BasicDBObject();
            $unwindTransactionData.put("path", "$" + tempNameTransaction);
            $unwindTransactionData.put("preserveNullAndEmptyArrays", true);

            BasicDBObject $match = new BasicDBObject();
            $match.put(tempNameTransaction + "." + ETransaction.status.name(), EStatusLifeCycle.Done.getName());

            if (privateId != null) {
                $match.put(EQuota.userValue.name(), privateId);
            }

            if (monitoringKey != null) {
                $match.put(EQuota._id.name(), monitoringKey);
            }
            if (quotaExpire) {
                $match.put(EQuota.expireDate.name(), new BasicDBObject("$lte", new Date()));
            }


            BasicDBObject $group = new BasicDBObject();
            BasicDBObject _id = new BasicDBObject();
            _id.put(EQuota.monitoringKey.name(), "$" + EQuota.monitoringKey.name());
            _id.put(EResourceQuota.resourceId.name(), "$" + EQuota.resources.name() + "." + EResourceQuota.resourceId.name());

            BasicDBObject tid = new BasicDBObject();
            tid.put("$push", "$" + tempNameTransaction + "." + ETransaction.tid.name());

            $group.put("_id", _id);
            $group.put(tempNameTids, tid);

            BasicDBObject $project = new BasicDBObject();

            BasicDBObject $cond = new BasicDBObject();
            $cond.put("if", new BasicDBObject("$isArray", "$" + tempNameTids));
            $cond.put("then", new BasicDBObject("$size", "$" + tempNameTids));
            $cond.put("else", "");

            $project.put(tempNameTids, "$" + tempNameTids);
            $project.put("count", new BasicDBObject("$cond", $cond));


            StringBuilder stringBuilder = new StringBuilder();
            List<DBObject> pipeline = Arrays.asList(
                    new BasicDBObject("$unwind", "$" + EQuota.resources.name()), //unwind resources
                    new BasicDBObject("$lookup", $lookupTransaction), //join transaction by resourceId
                    new BasicDBObject("$unwind", $unwindTransactionData), //unwind transactionData
                    new BasicDBObject("$match", $match),//status Done
                    new BasicDBObject("$group", $group),
                    new BasicDBObject("$project", $project));


            pipeline.forEach(dbObject -> {
                stringBuilder.append(gson.toJson(dbObject));
            });

            writeQueryLog("aggregate", Config.COLLECTION_QUOTA_NAME + "&" + Config.COLLECTION_TRANSACTION_NAME, stringBuilder.toString());


            Iterator<DBObject> dataIterator = db.getCollection(Config.COLLECTION_QUOTA_NAME).aggregate(pipeline).results().iterator();


            List<CommitData> commitData = new ArrayList<>();


            while (dataIterator.hasNext()) {
                commitData.add(gson.fromJson(gson.toJson(dataIterator.next()), CommitData.class));
            }

            appInstance.getPcefInstance().setCommitData(commitData);
        } catch (Exception e) {
            AFLog.d("find data to commit error" + e.getStackTrace()[0]);
        }

    }


}
