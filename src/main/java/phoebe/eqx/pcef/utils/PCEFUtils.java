package phoebe.eqx.pcef.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import ec02.af.abstracts.AbstractAF;
import ec02.af.utils.AFLog;
import phoebe.eqx.pcef.enums.config.EConfig;
import phoebe.eqx.pcef.enums.stats.EStatCmd;
import phoebe.eqx.pcef.enums.stats.EStatMode;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.*;

public class PCEFUtils {

    public static final SimpleDateFormat startStopDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    public static final SimpleDateFormat dtLongFormatterMs = new SimpleDateFormat("yyyyMMddHHmmss SSSS", Locale.US);
    public static final SimpleDateFormat isoDateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

   /* static {
        isoDateFormatter.setTimeZone(TimeZone.getTimeZone("Asia/Bangkok"));
    }*/

    public static final SimpleDateFormat transactionDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

    public static String getWarmConfig(AbstractAF abstractAF, EConfig eConfig) {
        try {
            return abstractAF.getEquinoxUtils().getHmWarmConfig().get(eConfig.getConfigName()).get(0);
        } catch (Exception e) {
            AFLog.d("GET WARM CONFIG name = " + eConfig.getConfigName() + "-" + e.getStackTrace()[0]);
            return eConfig.getDefaultData();
        }
    }

    public static void increaseStatistic(AbstractAF abstractAF, EStatMode eMode, EStatCmd eCmd) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(eCmd.getCmd()).append(" ");
        stringBuilder.append(eMode.getMode());
        abstractAF.getEquinoxUtils().incrementStats(stringBuilder.toString());
    }

    public static String gsonToJson(Object object) {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        return gson.toJson(object);
    }

    public static void writeLog(AbstractAF abstractAF, String req, String res, String summaryLog, Date startTime, String msg) {
        try {
            if (msg == null) {
                msg = "NONE";
            }

            long time = 0;
            if (startTime != null) {
                time = startTime.getTime();
            }
            AFLog.d("REQ=" + req);
            AFLog.d("RES=" + res);
            AFLog.d("SUMMARY=" + summaryLog);

            Calendar calendar = Calendar.getInstance();
            long usedTime = calendar.getTimeInMillis() - time;

            abstractAF.getEquinoxUtils().writeLog("PCEF_APP_LOG", "Request : " + req.trim());
            abstractAF.getEquinoxUtils().writeLog("PCEF_APP_LOG", "Response : " + res.trim());
            abstractAF.getEquinoxUtils().writeLog("PCEF_APP_LOG", "Summary : " + summaryLog.trim());
            abstractAF.getEquinoxUtils().writeLog("PCEF_APP_LOG", String.format("Start Time : %s, Used Time : %s, %s", startStopDateFormat.format(startTime), usedTime, msg));
        } catch (Exception e) {
            AFLog.d("WriteLog failed", e);
        }
    }


    public static String randomNumber3Digit() {
        return String.valueOf(new Random().nextInt(100));
    }

    public static Date getDate(int addSec) {
        Calendar calendar = Calendar.getInstance();
        if (addSec > 0) {
            calendar.add(Calendar.SECOND, addSec);
        }
        return calendar.getTime();
    }

    public static String getValueFromJson(String key, String json) {
        Type type = new TypeToken<Map<String, Object>>() {
        }.getType();
        Map<String, String> map = new GsonBuilder().create().fromJson(json, type);
        return map.get(key);
    }


    public static void writeMessageFlow(String message, MessageFlow.Status status, String sessionId) {
        MessageFlow messageFlow = new MessageFlow(message, status.name(), sessionId);
        AFLog.d("[MESSAGE_FLOW] : " + gsonToJson(messageFlow));
    }


}







