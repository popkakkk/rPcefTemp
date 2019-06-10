package phoebe.eqx.pcef.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import ec02.af.abstracts.AbstractAF;
import ec02.af.utils.AFLog;
import phoebe.eqx.pcef.DBResult;
import phoebe.eqx.pcef.core.cdr.Opudr;
import phoebe.eqx.pcef.enums.DBOperation;
import phoebe.eqx.pcef.enums.EEvent;
import phoebe.eqx.pcef.enums.config.EConfig;
import phoebe.eqx.pcef.enums.stats.EStatCmd;
import phoebe.eqx.pcef.enums.stats.EStatMode;
import phoebe.eqx.pcef.instance.Config;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.net.Inet4Address;
import java.text.SimpleDateFormat;
import java.util.*;

public class PCEFUtils {


    public static String HOST_NAME;
    public static final SimpleDateFormat regularDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    public static final SimpleDateFormat cdrDateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
    public static final SimpleDateFormat datetimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    public static final SimpleDateFormat dtLongFormatterMs = new SimpleDateFormat("yyyyMMddHHmmss SSSS", Locale.US);
    public static final SimpleDateFormat isoDateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    public static final SimpleDateFormat actualTimeDFM = new SimpleDateFormat("yyyyMMddHHmmss");

    static {
        isoDateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        HOST_NAME = getHostName();
    }


    private static String getHostName() {
        try {
            return Inet4Address.getLocalHost().getHostName();
        } catch (Exception e) {
            try {
                String hostname;
                Runtime runtime = Runtime.getRuntime();
                Process process = runtime.exec("hostname");
                BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
                hostname = br.readLine();
                process.getErrorStream().close();
                process.getOutputStream().close();
                br.close();
                return hostname;
            } catch (Exception ee) {
                return "UNKNOWNHOST";
            }
        }
    }


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
        String str = "rPCEF " + eCmd.getCmd() + " " + eMode.getMode();
        abstractAF.getEquinoxUtils().incrementStats(str);
    }

    public static String gsonToJson(Object object) {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        return gson.toJson(object);
    }

    public static EEvent getEventByRet(String ret) {
        EEvent event = null;
        if ("0".equals(ret)) {
            event = EEvent.SUCCESS;
        } else if ("1".equals(ret)) {
            event = EEvent.EquinoxMessageResponseError;
        } else if ("2".equals(ret)) {
            event = EEvent.EquinoxMessageResponseReject;
        } else if ("3".equals(ret)) {
            event = EEvent.EquinoxMessageResponseAbort;
        }
        return event;
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

            abstractAF.getEquinoxUtils().writeLog("rPCEF_APP_LOG", "Request : " + req.trim());
            abstractAF.getEquinoxUtils().writeLog("rPCEF_APP_LOG", "Response : " + res.trim());
            abstractAF.getEquinoxUtils().writeLog("rPCEF_APP_LOG", "Summary : " + summaryLog.trim());
            abstractAF.getEquinoxUtils().writeLog("rPCEF_APP_LOG", String.format("Start Time : %s, Used Time : %s, %s", regularDateFormat.format(startTime), usedTime, msg));
        } catch (Exception e) {
            AFLog.d("WriteLog failed", e);
        }
    }

  /*  public static void writeErrorLog(AbstractAF abstractAF, PCEFException e) {
        AFLog.d("Write Error Log ..");

        //build
       *//* ErrorLog errorLog = new ErrorLog(e.getError().getCode(), e.getError().getDesc() + "-" + e.getErrorMsg());
        String errorStr = gsonToJson(errorLog);

        //write
        AFLog.e(errorStr);
        abstractAF.getEquinoxUtils().writeLog(Config.LOG_ERROR_NAME, errorStr);*//*
    }*/


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

    public static String getObjecFromJson(String key, String json) {
        Type type = new TypeToken<Map<String, Object>>() {
        }.getType();
        Map<String, String> map = new GsonBuilder().create().fromJson(json, type);
        return map.get(key);
    }


    public static void writeMessageFlow(String message, MessageFlow.Status status, String sessionId) {
        MessageFlow messageFlow = new MessageFlow(message, status.name(), sessionId);
        AFLog.d("[MESSAGE_FLOW] : " + gsonToJson(messageFlow));
    }


    public static void writeDBMessageRequest(String collection, DBOperation dbOperation, Object query) {
        DBMsgRequest msgRequest = new DBMsgRequest();
        msgRequest.setDatabase(Config.MY_DB_NAME);
        msgRequest.setColletion(collection);
        msgRequest.setOperation(dbOperation.name());
        msgRequest.setQuery(query);
        AFLog.d("[DATABASE] : " + gsonToJson(msgRequest));
    }


    public static void writeDBMessageResponse(DBResult dbResult, int totalRecord, List<Object> objects) {
        DBMsgResponse dbMsgResponse = new DBMsgResponse();
        dbMsgResponse.setCode(dbResult.getCode());
        dbMsgResponse.setErrMsg(dbResult.getDesc());
        dbMsgResponse.setTotalRecord(totalRecord);
        if (objects != null) {
            dbMsgResponse.setResult(objects);
        }
        AFLog.d("[DATABASE] : " + gsonToJson(dbMsgResponse));
    }


    public static String generateCdr(Opudr opudr) throws JAXBException {

        AFLog.d("Generate CDR");

        StringWriter stringWriter = new StringWriter();

        JAXBContext jaxbContext = JAXBContext.newInstance(Opudr.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);

        marshaller.marshal(opudr, stringWriter);

        return stringWriter.toString();


    }


}







