package phoebe.eqx.pcef.enums.config;

import ec02.af.abstracts.AbstractAF;
import ec02.af.utils.AFLog;

public enum EConfig {

    /*Part 1 : Mandatory */
    //Resource
    RESOURCE_NAME_TEST("resource-name-test", EConfigType.MANDATORY, null),

    MONGODB_URL("mongodb-url", EConfigType.MANDATORY, null),
    MONGODB_DATABASE_NAME("mongodb-database-name", EConfigType.MANDATORY, null),
    INTERVAL_PROCESSING("interval-processing", EConfigType.MANDATORY, null),
    RETRY_PROCESSING("retry-processing", EConfigType.MANDATORY, null),

    //Collection name
    COLLECTION_TRANSACTION_NAME("collection-transaction-name", EConfigType.MANDATORY, null),
    COLLECTION_PROFILE_NAME("collection-profile-name", EConfigType.MANDATORY, null),
    COLLECTION_QUOTA_NAME("collection-quota-name", EConfigType.MANDATORY, null),



    /* Part 2 : Optional  */

    //Timeout
    TIMEOUT_TEST("timeout-test", EConfigType.OPTIONAL, "10"),
    ;

    private final String configName;
    private final EConfigType configType;
    private final String defaultData;

    EConfig(String configName, EConfigType configType, String defaultData) {
        this.configName = configName;
        this.configType = configType;
        this.defaultData = defaultData;
    }




    public String getConfigName() {
        return this.configName;
    }

    public EConfigType getConfigType() {
        return configType;
    }

    public String getDefaultData() {
        return defaultData;
    }


}