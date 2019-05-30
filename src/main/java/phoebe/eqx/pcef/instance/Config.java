package phoebe.eqx.pcef.instance;

import ec02.af.abstracts.AbstractAF;
import ec02.af.utils.AFLog;
import phoebe.eqx.pcef.enums.config.EConfig;
import phoebe.eqx.pcef.enums.config.EConfigType;
import phoebe.eqx.pcef.utils.PCEFUtils;

import javax.print.DocFlavor;

public class Config {
    public static String MONGODB_URL;
    public static String MY_DB_NAME;
    public static int INTERVAL_PROCESSING;
    public static int RETRY_PROCESSING;

    //collection name
    public static String COLLECTION_TRANSACTION_NAME;
    public static String COLLECTION_PROFILE_NAME;
    public static String COLLECTION_QUOTA_NAME;

    public static String URL_USAGE_MONITORING;
    public static String URL_PRODUCT;
    public static String URL_OCF_USAGE_MONITORING;

    public static String RESOURCE_NAME_OCF;
    public static String RESOURCE_NAME_PRODUCT;
    public static String RESOURCE_NAME_SACF;
    public static String RESOURCE_NAME_REFUND;


    public static String TIMEOUT_OCF;
    public static String TIMEOUT_PRODUCT;

    public static int RETRY_OCF_USAGE_MONITORING;
    public static int RETRY_PRODUCT_GET_RESOURCE_ID;

    public static String LOG_SUMMARY_NAME;
    public static String LOG_ERROR_NAME;

    public static String CDR_CHARGING_NAME;
    public static String CDR_REFUND_NAME;

    public static int DELAY_TIMEOUT;

    public static void loadConfiguration(AbstractAF abstractAF) {

        MONGODB_URL = PCEFUtils.getWarmConfig(abstractAF, EConfig.MONGODB_URL);
        MY_DB_NAME = PCEFUtils.getWarmConfig(abstractAF, EConfig.MONGODB_DATABASE_NAME);
        INTERVAL_PROCESSING = Integer.parseInt(PCEFUtils.getWarmConfig(abstractAF, EConfig.INTERVAL_PROCESSING));
        RETRY_PROCESSING = Integer.parseInt(PCEFUtils.getWarmConfig(abstractAF, EConfig.RETRY_PROCESSING));
        COLLECTION_PROFILE_NAME = PCEFUtils.getWarmConfig(abstractAF, EConfig.COLLECTION_PROFILE_NAME);
        COLLECTION_QUOTA_NAME = PCEFUtils.getWarmConfig(abstractAF, EConfig.COLLECTION_QUOTA_NAME);
        COLLECTION_TRANSACTION_NAME = PCEFUtils.getWarmConfig(abstractAF, EConfig.COLLECTION_TRANSACTION_NAME);


        URL_PRODUCT = PCEFUtils.getWarmConfig(abstractAF, EConfig.URL_PRODUCT);
        URL_OCF_USAGE_MONITORING = PCEFUtils.getWarmConfig(abstractAF, EConfig.URL_OCF_USAGE_MONITORING);

        RESOURCE_NAME_PRODUCT = PCEFUtils.getWarmConfig(abstractAF, EConfig.RESOURCE_NAME_PRODUCT);
        RESOURCE_NAME_SACF = PCEFUtils.getWarmConfig(abstractAF, EConfig.RESOURCE_NAME_SACF);
        RESOURCE_NAME_OCF = PCEFUtils.getWarmConfig(abstractAF, EConfig.RESOURCE_NAME_OCF);
        RESOURCE_NAME_REFUND = PCEFUtils.getWarmConfig(abstractAF, EConfig.RESOURCE_NAME_REFUND);

        TIMEOUT_OCF = PCEFUtils.getWarmConfig(abstractAF, EConfig.TIMEOUT_OCF);
        TIMEOUT_PRODUCT = PCEFUtils.getWarmConfig(abstractAF, EConfig.TIMEOUT_PRODUCT);

        RETRY_OCF_USAGE_MONITORING = Integer.parseInt(PCEFUtils.getWarmConfig(abstractAF, EConfig.RETRY_OCF_USAGE_MONITORING));
        RETRY_PRODUCT_GET_RESOURCE_ID = Integer.parseInt(PCEFUtils.getWarmConfig(abstractAF, EConfig.RETRY_PRODUCT_GET_RESOURCE_ID));

        LOG_SUMMARY_NAME = PCEFUtils.getWarmConfig(abstractAF, EConfig.RETRY_PRODUCT_GET_RESOURCE_ID);
        LOG_ERROR_NAME = PCEFUtils.getWarmConfig(abstractAF, EConfig.LOG_ERROR_NAME);

        CDR_CHARGING_NAME = PCEFUtils.getWarmConfig(abstractAF, EConfig.CDR_CHARGING_NAME);
        CDR_REFUND_NAME = PCEFUtils.getWarmConfig(abstractAF, EConfig.CDR_REFUND_NAME);

        DELAY_TIMEOUT = Integer.parseInt(PCEFUtils.getWarmConfig(abstractAF, EConfig.DELAY_TIMEOUT));
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
