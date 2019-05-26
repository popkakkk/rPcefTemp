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

import java.lang.reflect.Array;
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
        writeQueryLog("insert", collectionName, basicDBObject.toString());
        db.getCollection(collectionName).insert(basicDBObject);
    }

    public void insertManyByObject(List<BasicDBObject> insertList) {
        writeQueryLog("insert many", collectionName, insertList.toString());
        db.getCollection(collectionName).insert(insertList);
    }


    public DBCursor findByQuery(BasicDBObject whereQuery) {
        writeQueryLog("find", collectionName, whereQuery.toString());
        return db.getCollection(collectionName).find(whereQuery);
    }

    public void updateSetByQuery(BasicDBObject searchQuery, BasicDBObject updateQuery) {
        BasicDBObject setUpdate = new BasicDBObject("$set", updateQuery);
        writeQueryLog("update", collectionName, searchQuery.toString() + "," + setUpdate.toString());
        db.getCollection(collectionName).update(searchQuery, setUpdate);
    }


    public Iterable<DBObject> aggregateMatch(BasicDBObject match, BasicDBObject group) {
        List<DBObject> pipeline = Arrays.asList(
                new BasicDBObject("$match", match)
                , new BasicDBObject("$group", group));


        writeQueryLog("aggregate", collectionName, pipeline.toString());
        return db.getCollection(collectionName).aggregate(pipeline).results();
    }


    public DBObject findAndModify(BasicDBObject query, BasicDBObject update) {
        BasicDBObject setUpdate = new BasicDBObject("$set", update);
        writeQueryLog("aggregate", collectionName, query.toString() + "," + setUpdate.toString());
        return db.getCollection(collectionName).findAndModify(query, setUpdate);
    }


    public void writeQueryLog(String action, String collection, String condition) {
        AFLog.d("[Query] " + action + ",collection name:" + collection + ", query =" + condition);
    }


    public List<CommitData> findDataToCommit(String privateId, String monitoringKey, boolean quotaExpire) {

        try {

            String tempNameTransaction = "transactionData";
            String tempNameTids = "transactionIds";

            BasicDBObject $lookupTransaction = new BasicDBObject();
            $lookupTransaction.put("from", Config.COLLECTION_TRANSACTION_NAME);
            $lookupTransaction.put("let", new BasicDBObject(EResourceQuota.resourceId.name(), "$" + EQuota.resources.name() + "." + EResourceQuota.resourceId.name()));

            List<BasicDBObject> lookupCondition = new ArrayList<>();
            lookupCondition.add(new BasicDBObject("$eq", new String[]{"$" + ETransaction.resourceId.name(), "$$" + EResourceQuota.resourceId.name()}));
            lookupCondition.add(new BasicDBObject("$eq", new String[]{"$" + ETransaction.status.name(), EStatusLifeCycle.Done.getName()}));

            BasicDBObject lookupExpression = new BasicDBObject();
            lookupExpression.put("$match", new BasicDBObject("$expr", new BasicDBObject("$and", lookupCondition)));

            $lookupTransaction.put("pipeline", new BasicDBObject[]{lookupExpression});
            $lookupTransaction.put("as", tempNameTransaction);


            BasicDBObject $unwindTransactionData = new BasicDBObject();
            $unwindTransactionData.put("path", "$" + tempNameTransaction);
            $unwindTransactionData.put("preserveNullAndEmptyArrays", true);

            BasicDBObject $match = new BasicDBObject();
            if (privateId != null) {
                $match.put(EQuota.userValue.name(), privateId);
            }

            if (monitoringKey != null) {
                $match.put(tempNameTransaction + "." + ETransaction.monitoringKey.name(), monitoringKey);
            }
            if (quotaExpire) {
                $match.put(EQuota.expireDate.name(), new BasicDBObject("$lte", new Date()));
            }


            BasicDBObject $group = new BasicDBObject();
            BasicDBObject _id = new BasicDBObject();
            _id.put(EQuota.monitoringKey.name(), "$" + EQuota.monitoringKey.name());
            _id.put(EResourceQuota.resourceId.name(), "$" + EQuota.resources.name() + "." + EResourceQuota.resourceId.name());
            _id.put(EResourceQuota.resourceName.name(), "$" + EQuota.resources.name() + "." + EResourceQuota.resourceName.name());

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


            List<DBObject> pipeline = Arrays.asList(
                    new BasicDBObject("$unwind", "$" + EQuota.resources.name()), //unwind resources
                    new BasicDBObject("$lookup", $lookupTransaction), //join transaction by resourceId
                    new BasicDBObject("$unwind", $unwindTransactionData), //unwind transactionData
                    new BasicDBObject("$match", $match),//status Done
                    new BasicDBObject("$group", $group),
                    new BasicDBObject("$project", $project));

            writeQueryLog("aggregate", Config.COLLECTION_QUOTA_NAME, pipeline.toString());


            Iterator<DBObject> dataIterator = db.getCollection(Config.COLLECTION_QUOTA_NAME).aggregate(pipeline).results().iterator();


            List<CommitData> commitDatas = new ArrayList<>();


            while (dataIterator.hasNext()) {
                commitDatas.add(gson.fromJson(gson.toJson(dataIterator.next()), CommitData.class));
            }

            return commitDatas;
        } catch (Exception e) {
            AFLog.d("find data to commit error" + e.getStackTrace()[0]);
            throw e;
        }


    }


}
