package phoebe.eqx.pcef.instance;

import ec02.af.abstracts.AbstractAF;
import ec02.af.utils.AFLog;
import phoebe.eqx.pcef.enums.config.EConfig;
import phoebe.eqx.pcef.enums.config.EConfigType;
import phoebe.eqx.pcef.utils.PCEFUtils;

public class Config {
    public static String TEST_CONFIG;
    public static String MONGODB_URL;
    public static String MY_DB_NAME;
    public static int INTERVAL_PROCESSING;
    public static int RETRY_PROCESSING;

    //collection name
    public static String COLLECTION_TRANSACTION_NAME;
    public static String COLLECTION_PROFILE_NAME;
    public static String COLLECTION_QUOTA_NAME;


    public static void loadConfiguration(AbstractAF abstractAF) {
        TEST_CONFIG = PCEFUtils.getWarmConfig(abstractAF, EConfig.RESOURCE_NAME_TEST);
        MONGODB_URL = PCEFUtils.getWarmConfig(abstractAF, EConfig.MONGODB_URL);
        MY_DB_NAME = PCEFUtils.getWarmConfig(abstractAF, EConfig.MONGODB_DATABASE_NAME);
        INTERVAL_PROCESSING = Integer.parseInt(PCEFUtils.getWarmConfig(abstractAF, EConfig.INTERVAL_PROCESSING));
        RETRY_PROCESSING = Integer.parseInt(PCEFUtils.getWarmConfig(abstractAF, EConfig.RETRY_PROCESSING));
        COLLECTION_PROFILE_NAME = PCEFUtils.getWarmConfig(abstractAF, EConfig.COLLECTION_PROFILE_NAME);
        COLLECTION_QUOTA_NAME = PCEFUtils.getWarmConfig(abstractAF, EConfig.COLLECTION_QUOTA_NAME);
        COLLECTION_TRANSACTION_NAME = PCEFUtils.getWarmConfig(abstractAF, EConfig.COLLECTION_TRANSACTION_NAME);


    }

    public static boolean verify(AbstractAF abstractAF) {
        boolean result = true;

        try {
            for (EConfig eConfig : EConfig.values()) {
                String confName = eConfig.getConfigName().trim();
                if ((EConfigType.MANDATORY).equals(eConfig.getConfigType())) {
                    if (abstractAF.getEquinoxUtils().getHmWarmConfig().containsKey(confName)) {
                        String confValue = abstractAF.getEquinoxUtils().getHmWarmConfig().get(confName).get(0);
                        if ("".equals(confValue)) {
                            AFLog.w("[MANDATORY] Config : " + confName + ", IS_EMPTY!!!!");
                        }
                    } else {
                        AFLog.e("[MANDATORY] Config : " + confName + ", NOT_FOUND!!!!");
                        result = false;
                    }
                }
            }
        } catch (Exception e) {
            result = false;
        }

        AFLog.d("----- Verify AF Configuration [" + result + "] -----");
        return result;
    }

}
