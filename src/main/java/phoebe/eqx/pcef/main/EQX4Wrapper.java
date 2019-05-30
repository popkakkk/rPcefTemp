package phoebe.eqx.pcef.main;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.DBCursor;
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
import phoebe.eqx.pcef.core.exceptions.PCEFException;
import phoebe.eqx.pcef.core.model.Profile;
import phoebe.eqx.pcef.core.model.Transaction;
import phoebe.eqx.pcef.enums.ERequestType;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.instance.context.RequestContext;
import phoebe.eqx.pcef.services.VTTimoutService;
import phoebe.eqx.pcef.services.mogodb.MongoDBConnect;
import phoebe.eqx.pcef.states.L1.W_E11_TIMEOUT;
import phoebe.eqx.pcef.states.L1.W_GyRAR;
import phoebe.eqx.pcef.states.L1.W_REFUND_MANAGEMENT;
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
            /*if (appInstance == null) {
                throw new Exception("Instance is null");
            }


*/

            AFLog.d("--------------------- [ Start Before Process ] ---------------------");
            //handle incoming message
            boolean process = beforeProcess(equinoxProperties, abstractAF, equinoxRawDatas, appInstance);
            AFLog.d("--------------------- [ Stop Before Process  ] ---------------------");
            AFLog.d("--------------------- [   Start Do Process   ] ---------------------");
            if (process) doProcess(abstractAF, appInstance);
            AFLog.d("--------------------- [    Stop Do Process   ] ---------------------");
            AFLog.d("--------------------- [ Start After Process  ] ---------------------");
            //handle outgoing message
            afterProcess(eqxPropOut, abstractAF, equinoxRawDatas, appInstance, equinoxProperties);
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

            if (appInstance.isFinish()) {
                return "";
            }

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
            RequestContext requestContext = null;

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
                            && url.equalsIgnoreCase("/rpcef/v1/metering-method")) {

                        String val = rawData.getRawDataAttribute("val");
                        requestContext = new RequestContext(val, invoke, equinoxPropertiesAF.getSession(), ERequestType.USAGE_MONITORING);
                        appInstance.getRequestContexts().add(requestContext);
                    } else if (name.equalsIgnoreCase("http")
                            && cType.equalsIgnoreCase("text/plain")
                            && method.equalsIgnoreCase("post")
                            && url.equalsIgnoreCase("/rpcef/v1/gyrar")) {

                        String val = rawData.getRawDataAttribute("val");
                        requestContext = new RequestContext(val, invoke, equinoxPropertiesAF.getSession(), ERequestType.GyRAR);
                        appInstance.getRequestContexts().add(requestContext);

                    } else if (name.equalsIgnoreCase("http")
                            && cType.equalsIgnoreCase("text/plain")
                            && method.equalsIgnoreCase("post")
                            && url.equalsIgnoreCase("/rpcef/v1/refund-management")) {

                        String val = rawData.getRawDataAttribute("val");
                        requestContext = new RequestContext(val, invoke, equinoxPropertiesAF.getSession(), ERequestType.REFUND_MANAGEMENT);
                        appInstance.getRequestContexts().add(requestContext);

                    }
                } else if ("response".equals(type)) {
                    requestContext = appInstance.findCompleteContextListMatchResponse(rawData, ret);
                    process = requestContext.getInvokeManager().dataResponseComplete();
                }
            }


            /***** Timeout and retry timeout *****/
            if (equinoxPropertiesAF.isTimeout()) {

                /**Check E11 timeout vt exhaust**/
                if (appInstance.getRequestContexts().size() == 0) {
                    requestContext = new RequestContext("", "", equinoxPropertiesAF.getSession(), ERequestType.E11_TIMEOUT);
                    AFLog.d("[E11 TIMEOUT] Snap Datetime : " + PCEFUtils.datetimeFormat.format(requestContext.getPcefInstance().getStartTime()));
                    AFLog.d("[E11 TIMEOUT] Snap ISODate : " + PCEFUtils.isoDateFormatter.format(requestContext.getPcefInstance().getStartTime()));
                    appInstance.getRequestContexts().add(requestContext);
                } else {

                    List<RequestContext> requestContextList = appInstance.getRequestContexts();
                    for (RequestContext context : requestContextList) {

                    }


                   /* InvokeManager invokeManager = appInstance.getInvokeManager();
                    if (!invokeManager.retryTimeout(appInstance)) {
                        //set event timeout
                        invokeManager.setEventTimeout();
                    } else {
                        //retry success
                        process = false;
                    }*/
                }

            }

            appInstance.setMyContext(requestContext);

            ERequestType requestType = requestContext.getRequestType();
            AFLog.d("[Request Type]: " + requestType);
            if (requestType == null) {
                throw new Exception("Unknown request Type from message");
            }

        } catch (Exception e) {
            AFLog.d("Before process error" + e.getStackTrace()[0]);
            process = false;
        }
        return process;
    }


    private static void doProcess(AbstractAF abstractAF, AppInstance appInstance) {
        try {
            appInstance.setAbstractAF(abstractAF);
            ERequestType requestType = appInstance.getMyContext().getRequestType();

            State state = null;
            switch (requestType) {
                case USAGE_MONITORING:
                    state = new W_USAGE_MONITORING(appInstance);
                    break;
                case E11_TIMEOUT:
                    state = new W_E11_TIMEOUT(appInstance);
                    break;
                case GyRAR:
                    state = new W_GyRAR(appInstance);
                    break;
                case REFUND_MANAGEMENT:
                    state = new W_REFUND_MANAGEMENT(appInstance);
                    break;
            }
            state.dispatch();

            appInstance.patchResponse();


        } catch (Exception e) {
            AFLog.e("Do process error", e);
        }
    }


    private static void afterProcess(EquinoxPropertiesAF eqxPropOut, AbstractAF
            abstractAF, ArrayList<EquinoxRawData> equinoxRawDatas, AppInstance appInstance, EquinoxPropertiesAF equinoxPropertiesAF) {
        try {


//            if (appInstance.getMyContext().getPcefException() != null) {
//                writeErrorLog(abstractAF, appInstance);
//                buildErrorResponse(appInstance, equinoxPropertiesAF);
//            }


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
            int timeout = calculateMinQueryTimeout(appInstance.getOutList());
            eqxPropOut.setTimeout(String.valueOf(timeout));
            AFLog.d("EquinoxProperties timeout =" + timeout);

            //set Ret
            if (appInstance.isFinish()) {
//                writeSummaryLog(eqxPropOut, abstractAF, equinoxRawDatas, appInstance);
                eqxPropOut.setState(EStateApp.IDLE.getName());
                eqxPropOut.setRet(EEquinoxMessage.Ret.END);
                AFLog.d("set State:" + EStateApp.IDLE.getName() + ", set Ret:" + EEquinoxMessage.Ret.END);
            } else {
                eqxPropOut.setState(EStateApp.ACTIVE.getName());
                eqxPropOut.setRet(EEquinoxMessage.Ret.NORMAL);
                AFLog.d("set State:" + EStateApp.ACTIVE.getName() + ", set Ret:" + EEquinoxMessage.Ret.NORMAL);
            }


            //clear request context
            boolean response = false;
            if (outList.size() == 1) {
                if (outList.get(0).getType().equals("response")) {
                    response = true;
                }
            }

            if (response) {
                AFLog.d("clear context:" + appInstance.getMyContext().getRequestType());
                appInstance.removeRequestContext();
            }


        } catch (Exception e) {
            AFLog.e("After process error", e);
        }
    }


    private static void writeErrorLog(AbstractAF abstractAF, AppInstance appInstance) {
        ERequestType requestType = appInstance.getMyContext().getRequestType();
        PCEFException pcefException = appInstance.getMyContext().getPcefException();

        if (requestType.equals(ERequestType.USAGE_MONITORING)) {
            String resourceId = "";
            Transaction transaction = appInstance.getMyContext().getPcefInstance().getTransaction();
            if (transaction != null) {
                resourceId = transaction.getResourceId();
            }
            WriteLog.writeErrorLogUsageMonitoring(appInstance.getAbstractAF(), pcefException, appInstance.getMyContext().getPcefInstance().getUsageMonitoringRequest(), resourceId);
        } else if (requestType.equals(ERequestType.E11_TIMEOUT)) {
            Profile profile = appInstance.getMyContext().getPcefInstance().getProfile();
            String sessionId = "";
            String userType = "";
            String userValue = "";

            if (profile != null) {
                sessionId = profile.getSessionId();
                userType = profile.getUserType();
                userValue = profile.getUserValue();
            }

            WriteLog.writeErrorE11Timeout(appInstance.getAbstractAF(), pcefException, sessionId, userType, userValue);
        } else if (requestType.equals(ERequestType.REFUND_MANAGEMENT)) {
            WriteLog.writeErrorLogRefundManagement(appInstance.getAbstractAF(), pcefException, appInstance.getMyContext().getPcefInstance().getRefundManagementRequest());
        } else if (requestType.equals(ERequestType.GyRAR)) {
            WriteLog.writeErrorLogGyRAR(appInstance.getAbstractAF(), pcefException, appInstance.getMyContext().getPcefInstance().getGyRARRequest());
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

//        PCEFUtils.writeLog(abstractAF, requestLog, responseLog, summaryLog, appInstance.getStartTime(), "");
    }


    private static int calculateMinQueryTimeout(List<EquinoxRawData> outList) {
        Integer timeout = null;
        for (EquinoxRawData out : outList) {
            int timeoutRaw = Integer.parseInt(out.getRawDataAttribute("timeout"));
            if (timeout != null) {
                timeout = Math.min(timeout, timeoutRaw);
            } else {
                timeout = timeoutRaw;
            }
        }
        return (timeout == null) ? 10 : timeout;
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
