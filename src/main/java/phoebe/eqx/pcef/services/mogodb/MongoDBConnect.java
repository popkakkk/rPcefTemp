package phoebe.eqx.pcef.services.mogodb;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.instance.Config;

public class MongoDBConnect {

    private ProfileService profileService;
    private QuotaService quotaService;
    private TransactionService transactionService;

    private MongoClient mongoClient;

    public MongoDBConnect(AppInstance appInstance) {
        this.mongoClient = connectDB();
        this.profileService = new ProfileService(appInstance, mongoClient);
        this.quotaService = new QuotaService(appInstance, mongoClient);
        this.transactionService = new TransactionService(appInstance, mongoClient);
    }

    private static MongoClient connectDB() {
        return new MongoClient(new MongoClientURI(Config.MONGODB_URL));
    }

    public void closeConnection() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }


    public ProfileService getProfileService() {
        return profileService;
    }


    public QuotaService getQuotaService() {
        return quotaService;
    }


    public TransactionService getTransactionService() {
        return transactionService;
    }


    public MongoClient getMongoClient() {
        return mongoClient;
    }




}
