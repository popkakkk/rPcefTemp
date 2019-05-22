package phoebe.eqx.pcef.enums.config;

public enum EConfig {

    /*Part 1 : Mandatory */
    //ResourceResponse

    MONGODB_URL("mongodb-url", EConfigType.MANDATORY, null),
    MONGODB_DATABASE_NAME("mongodb-database-name", EConfigType.MANDATORY, null),
    INTERVAL_PROCESSING("interval-processing", EConfigType.MANDATORY, null),
    RETRY_PROCESSING("retry-processing", EConfigType.MANDATORY, null),

    //Collection name
    COLLECTION_TRANSACTION_NAME("collection-transaction-name", EConfigType.MANDATORY, null),
    COLLECTION_PROFILE_NAME("collection-profile-name", EConfigType.MANDATORY, null),
    COLLECTION_QUOTA_NAME("collection-quota-name", EConfigType.MANDATORY, null),
    URL_USAGE_MONITORING("url-usage-monitoring", EConfigType.MANDATORY, null),
    URL_PRODUCT("url-product", EConfigType.MANDATORY, null),

    //resource name
    RESOURCE_NAME_OCF("resource-name-ocf", EConfigType.MANDATORY, null),
    RESOURCE_NAME_PRODUCT("resource-name-product", EConfigType.MANDATORY, null),
    RESOURCE_NAME_SACF("resource-name-sacf", EConfigType.MANDATORY, null),



    /* Part 2 : Optional  */

    //timeout
    TIMEOUT_OCF("timeout-ocf", EConfigType.OPTIONAL, "10"),
    TIMEOUT_PRODUCT("timeout-product", EConfigType.OPTIONAL, "10"),


    //retry timeout
    RETRY_OCF_USAGE_MONITORING("retry-ocf-usage-monitoring", EConfigType.OPTIONAL, "1"),
    RETRY_PRODUCT_GET_RESOURCE_ID("retry-product-get-resource-id", EConfigType.OPTIONAL, "1");


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