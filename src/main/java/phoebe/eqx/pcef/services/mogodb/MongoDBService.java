package phoebe.eqx.pcef.services.mogodb;

import com.google.gson.Gson;
import com.mongodb.*;
import ec02.af.utils.AFLog;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.instance.Config;

import java.util.Arrays;
import java.util.List;

public class MongoDBService {

    protected AppInstance appInstance;
    protected DB db;
    protected String collectionName;

    public Gson gson = new Gson();


    public MongoDBService(AppInstance appInstance, MongoClient mongoClient, String collectionName) {
        this.db = mongoClient.getDB(Config.MY_DB_NAME);
        this.appInstance = appInstance;
        this.collectionName = collectionName;
    }


    public void insertByObject(String collectionName, Object object) {
        String json = gson.toJson(object);
        writeQueryLog("insert", collectionName, json);
        db.getCollection(collectionName).insert(BasicDBObject.parse(json));
    }

    public void insertByQuery(String collectionName, BasicDBObject basicDBObject) {
        db.getCollection(collectionName).insert(basicDBObject);
    }

    public void insertManyByObject(String collectionName, List<BasicDBObject> insertList) {
        writeQueryLog("insert many", collectionName, gson.toJson(insertList));
        db.getCollection(collectionName).insert(insertList);
    }

    public void updatePush(String collectionName, BasicDBObject searchQuery, BasicDBObject updateQuery) {
        BasicDBObject pushUpdate = new BasicDBObject("$push", updateQuery);
        String condition = "searchQuery:" + searchQuery.toJson() + ",updateQuery:" + pushUpdate.toJson();
        writeQueryLog("update", collectionName, condition);
        db.getCollection(collectionName).update(searchQuery, pushUpdate);
    }


    public DBCursor findByObject(String collectionName, Object object) {
        String json = gson.toJson(object);
        writeQueryLog("find", collectionName, json);
        return db.getCollection(collectionName).find(BasicDBObject.parse(json));
    }

    public DBCursor findByQuery(String collectionName, BasicDBObject whereQuery) {
        writeQueryLog("find", collectionName, whereQuery.toJson());
        return db.getCollection(collectionName).find(whereQuery);
    }

    public void updateSetByQuery(String collectionName, BasicDBObject searchQuery, BasicDBObject updateQuery) {
        BasicDBObject setUpdate = new BasicDBObject("$set", updateQuery);
        String condition = "searchQuery:" + searchQuery.toJson() + ",updateQuery:" + setUpdate.toJson();
        writeQueryLog("update", collectionName, condition);
        db.getCollection(collectionName).update(searchQuery, setUpdate);
    }


    public Iterable<DBObject> aggregateMatch(String collectionName, BasicDBObject match, BasicDBObject group) {
        List<DBObject> pipeline = Arrays.asList(
                new BasicDBObject("$match", match)
                , new BasicDBObject("$group", group));

        String condition = "$match:" + match.toJson() + ",$group:" + group.toJson();
        writeQueryLog("aggregate", collectionName, condition);
        return db.getCollection(collectionName).aggregate(pipeline).results();
    }

    public DBObject findAndModify(String collectionName, BasicDBObject query, BasicDBObject update) {
        BasicDBObject setUpdate = new BasicDBObject("$set", update);
        return db.getCollection(collectionName).findAndModify(query, setUpdate);
    }


    public List distinctByObject(String collectionName, String fieldName, Object object) {
        return db.getCollection(collectionName).distinct(fieldName, BasicDBObject.parse(gson.toJson(object)));
    }


    private void writeQueryLog(String action, String collection, String condition) {
        AFLog.d("[Query] " + action + " " + collection + ", condition =" + condition);
    }


}
