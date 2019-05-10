package phoebe.eqx.pcef.main;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ec02.af.abstracts.AbstractAF;
import ec02.af.data.AFDataFactory;
import ec02.af.utils.AFLog;
import ec02.data.ESxxDataFactory;
import ec02.data.enums.EEquinoxMessage;
import ec02.data.enums.EEquinoxRawData;
import ec02.data.interfaces.ECDialogue;
import ec02.data.interfaces.ESxxData;
import ec02.data.interfaces.EquinoxPropertiesAF;
import ec02.data.interfaces.EquinoxRawData;
import org.apache.commons.lang3.StringUtils;
import phoebe.eqx.pcef.instance.InvokeManager;
import phoebe.eqx.pcef.core.data.InvokeObject;
import phoebe.eqx.pcef.enums.EEvent;
import phoebe.eqx.pcef.enums.ERequestType;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.states.L1.W_USAGE_MONITORING;
import phoebe.eqx.pcef.states.abs.State;
import phoebe.eqx.pcef.utils.*;

import java.util.*;

public class EQX4Wrapper {

    private static Gson gson;

    static {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(ec02.data.interfaces.KeyObject.class, new InterfaceAdapter<ec02.data.interfaces.KeyObject>());
        gsonBuilder.registerTypeAdapter(EquinoxRawData.class, new InterfaceAdapter<EquinoxRawData>());
        gsonBuilder.registerTypeAdapter(com.google.gson.TypeAdapterFactory.class, new InterfaceAdapter<com.google.gson.TypeAdapterFactory>());
        gsonBuilder.registerTypeAdapter(com.google.gson.FieldNamingStrategy.class, new FieldNamingPolicyDeserializer());
        gsonBuilder.registerTypeAdapter(ec02.data.enums.EEventType.class, new EEventTypeDeserializer());
        gsonBuilder.registerTypeAdapter(ec02.data.interfaces.E01Data.class, new InterfaceAdapter<ec02.data.interfaces.E01Data>());
        gsonBuilder.registerTypeAdapter(Date.class, new JsonDateDeserializer());
        gson = gsonBuilder.create();
    }


    public static AppInstance extractInstance(String instance) {
        AppInstance appInstance = null;
        try {
            if (StringUtils.isNotBlank(instance)) {
                /*decode logic
                 */
                byte[] simpleString = Base64.getDecoder().decode(instance);
                byte[] unZipString = Zip.extractBytes(simpleString);
                instance = new String(unZipString);
                appInstance = gson.fromJson(instance, AppInstance.class);
            } else {
                appInstance = new AppInstance();
            }
        } catch (Exception e) {
            AFLog.e("Extract instance error", e);
        }
        return appInstance;
    }

    public static ECDialogue callActionProcess(EquinoxPropertiesAF equinoxProperties, AbstractAF abstractAF, ArrayList<EquinoxRawData> equinoxRawDatas, AppInstance appInstance) {
        EquinoxPropertiesAF eqxPropOut = AFDataFactory.createEquinoxProperties();
        try {
            if (appInstance == null) {
                throw new Exception("Instance is null");
            }

            AFLog.d("--------------------- [ Start Before Process ] ---------------------");
            //handle incoming message
            boolean process = beforeProcess(equinoxProperties, abstractAF, equinoxRawDatas, appInstance);
            AFLog.d("--------------------- [ Stop Before Process  ] ---------------------");
            AFLog.d("--------------------- [   Start Do Process   ] ---------------------");
            if (process) doProcess(abstractAF, appInstance);
            AFLog.d("--------------------- [    Stop Do Process   ] ---------------------");
            AFLog.d("--------------------- [ Start After Process  ] ---------------------");
            //handle outgoing message
            afterProcess(eqxPropOut, abstractAF, equinoxRawDatas, appInstance);
            AFLog.d("--------------------- [ Stop After Process   ] ---------------------");

        } catch (Exception e) {
            AFLog.w("Action process error", e);
            eqxPropOut.setRet(EEquinoxMessage.Ret.END);
            eqxPropOut.setState(EStateApp.IDLE.getName());
        }
        return AFDataFactory.createECDialogue(eqxPropOut);
    }


    public static String composeInstance(AppInstance appInstance) {
        try {
            /*
             * * encode logic
             * */
            String str = gson.toJson(appInstance);
            byte[] bytes = str.getBytes();
            byte[] zipBytes = Zip.compressBytes(bytes);
            return Base64.getEncoder().encodeToString(zipBytes);
        } catch (Exception e) {
            AFLog.e("Compose instance error", e);
            return "";
        }
    }

    private static boolean beforeProcess(EquinoxPropertiesAF equinoxPropertiesAF, AbstractAF abstractAF, ArrayList<EquinoxRawData> equinoxRawDatas, AppInstance appInstance) {
        boolean process = true;
        try {

            /***** Raw data[request,response] *****/
            for (EquinoxRawData rawData : equinoxRawDatas) {
                String ret = rawData.getRet();
                String type = rawData.getRawDataAttribute("type");

                if ("request".equals(type) && "0".equals(ret)) {
                    String name = rawData.getName();
                    String cType = rawData.getCType();
                    String method = rawData.getRawDataAttribute("method");
                    String url = rawData.getRawDataAttribute("url");
                    String invoke = rawData.getInvoke();

                    if (name.equalsIgnoreCase("http")
                            && cType.equalsIgnoreCase("text/plain")
                            && method.equalsIgnoreCase("post")
                            && url.equalsIgnoreCase("/rpcef/api/v1/metering-method")) {
                        //String cmd = PCEFUtils.getValueFromJson("command", val);

                        String val = rawData.getRawDataAttribute("val");
                        appInstance.create(val, invoke, ERequestType.USAGE_MONITORING);
                    }
                } else if ("response".equals(type)) {
                    InvokeManager invokeManager = appInstance.getInvokeManager();

                    //get event
                    EEvent event = getEventByRet(ret);

                    //put equinoxRawData
                    invokeManager.putRawData(rawData, event);

                    process = invokeManager.dataResponseComplete();
                }
            }

            /***** Timeout and retry timeout *****/
            if (equinoxPropertiesAF.isTimeout()) {
                InvokeManager invokeManager = appInstance.getInvokeManager();
                if (!invokeManager.retryTimeout(appInstance)) {
                    //set event timeout
                    invokeManager.setEventTimeout();
                } else {
                    //retry success
                    process = false;
                }
            }

        } catch (Exception e) {
            AFLog.e("Before process error", e);
            process = false;
        }
        return process;
    }


    private static EEvent getEventByRet(String ret) {
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


    private static void doProcess(AbstractAF abstractAF, AppInstance appInstance) {
        try {
            appInstance.setAbstractAF(abstractAF);
            ERequestType requestType = appInstance.getRequestType();
            AFLog.d("[Request Type]: " + requestType);

            State state = null;
            switch (requestType) {
                case USAGE_MONITORING:
                    state = new W_USAGE_MONITORING(appInstance);
            }
            if (state != null) {
                state.dispatch();
                appInstance.patchResponse();
            }

        } catch (Exception e) {
            AFLog.e("Do process error", e);
        }
    }

    private static void afterProcess(EquinoxPropertiesAF eqxPropOut, AbstractAF
            abstractAF, ArrayList<EquinoxRawData> equinoxRawDatas, AppInstance appInstance) {
        try {
            //sent Outgoing Message
            List<EquinoxRawData> outList = appInstance.getOutList();
            if (null != outList && 0 < outList.size()) {
                int i = 0;
                for (; i < outList.size(); i++) {
                    EquinoxRawData outListObj = outList.get(i);
                    Map<String, String> optionalAttribute = outListObj.getRawDataAttributes();

                    //Create Object Default
                    ESxxData eSxxData = ESxxDataFactory.createObject(EEquinoxRawData.Name.HTTP);
                    eSxxData.getEquinoxRawData().setRawDataAttributes(optionalAttribute);

                    eSxxData.getEquinoxRawData().setRawDataMessage(outListObj.getRawDataMessage());
                    abstractAF.getEquinoxUtils().getDataBuffer().putESxxDatas(eSxxData);
                }
            }

            //calculate min query timeout
            int timeout = calculateMinQueryTimeout(appInstance.getInvokeManager().getList());
            eqxPropOut.setTimeout(String.valueOf(timeout));

            //set Ret
            if (appInstance.isFinish()) {
                writeSummaryLog(eqxPropOut, abstractAF, equinoxRawDatas, appInstance);
                eqxPropOut.setState(EStateApp.IDLE.getName());
                eqxPropOut.setRet(EEquinoxMessage.Ret.END);
            } else {
                eqxPropOut.setState(EStateApp.ACTIVE.getName());
                eqxPropOut.setRet(EEquinoxMessage.Ret.NORMAL);
            }
        } catch (Exception e) {
            AFLog.e("After process error", e);
        }
    }

    private static void writeSummaryLog(EquinoxPropertiesAF equinoxPropertiesAF, AbstractAF
            abstractAF, ArrayList<EquinoxRawData> equinoxRawDatas, AppInstance appInstance) {
        //get Request Log
        String requestLog = appInstance.getRequestLog();
        //get Response Log
        String responseLog = appInstance.getResponseLog();
        //get Summary Log
        String summaryLog = appInstance.getSummaryLogStr();

        PCEFUtils.writeLog(abstractAF, requestLog, responseLog, summaryLog, appInstance.getStartTime(), "");
    }


    private static int calculateMinQueryTimeout(List<InvokeObject> invokeObjects) {
        int timeout = 10;
        for (InvokeObject invokeObject : invokeObjects) {
            if (invokeObject.getOperation() != null) {
                timeout = Math.min(timeout, Integer.parseInt(invokeObject.getOperationRawReq().getRawDataAttribute("timeout")));
            }
        }
        return timeout;
    }


    private enum EStateApp {
        IDLE("BEGIN"),
        ACTIVE("ACTIVE");

        private String name;

        EStateApp(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }


}
