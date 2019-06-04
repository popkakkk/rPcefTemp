package phoebe.eqx.pcef.states.abs;

import com.mongodb.DBCursor;
import ec02.af.utils.AFLog;
import phoebe.eqx.pcef.core.exceptions.PCEFException;
import phoebe.eqx.pcef.enums.ERequestType;
import phoebe.eqx.pcef.enums.state.EState;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.services.*;
import phoebe.eqx.pcef.services.mogodb.MongoDBConnect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public abstract class ComplexState extends State {
    private boolean stop;

    public ComplexState(AppInstance appInstance, Level level) {
        super(appInstance, level);
    }

    private Object dispatchToWorkingState(EState subState) throws PCEFException, InvocationTargetException, IllegalAccessException {
        AFLog.d("disPatch " + this.getClass().getSimpleName() + " " + subState);
        Object invoke = null;
        Class clazz = getClass();
        Method method = findMethod(clazz, subState);
        AFLog.d("Invoke " + method.getName());
        invoke = method.invoke(this);
        return invoke;
    }

    public void dispatch() {
        dispatch(null);
    }

    public void dispatch(StateHandler handler) {
        boolean continueWork = true;
        while (continueWork) {
            EState state = getWorkState();
            if (state == null || state.equals(EState.END)) {
                state = EState.BEGIN;
            }

            try {
                dispatchToWorkingState(state); //execute state
            } catch (Exception e) {

                buildErrorResponse(appInstance);
//                AFLog.e("working state error-" + e.getStackTrace()[0]);
                stop = true;
            }

            //reset interval retry
            if (!appInstance.getMyContext().isInterval()) {
                appInstance.getMyContext().setIntervalRetry(0);
            }

            EState workState = getWorkState();
            continueWork = !this.appInstance.getMyContext().isHasRequest();
            if (EState.END.equals(workState)) {
                if (handler != null) {
                    handler.onEnd();
                }
                AFLog.d("stop by set END stateWork ..");
                break;
            }


            if (stop) {
                AFLog.d("Stop...by..flag");
                break;
            }
        }

    }

    public static Method findMethod(Class clazz, EState state) {
        for (Method method : clazz.getMethods()) {
            if (method.isAnnotationPresent(MessageRecieved.class)) {
                MessageRecieved messageRecieved = method.getAnnotation(MessageRecieved.class);
                if (state.equals(messageRecieved.messageType())) {
                    return method;
                }
            }
        }
        return null;
    }

    public boolean isStop() {
        return stop;
    }

    public void setStop(boolean stop) {
        this.stop = stop;
    }


    private void buildErrorResponse(AppInstance appInstance) {
        ERequestType requestType = appInstance.getMyContext().getRequestType();

        try {
            //findProfile
            boolean foundProfile = false;

            if (appInstance.getMyContext().getPcefInstance().getProfile() != null) {
                foundProfile = true;
            } else {
                //find profile set profile

                MongoDBConnect dbConnect = null;
                try {
                    dbConnect = new MongoDBConnect(appInstance);
                    String privateId = null;

                    //getPrivateId
                    if (!requestType.equals(ERequestType.REFUND_MANAGEMENT)) {
                        privateId = appInstance.getMyContext().getEqxPropSession();
                    }

                    if (privateId != null) {
                        DBCursor dbCursor = dbConnect.getProfileService().findProfileByPrivateId(privateId);
                        if (dbCursor.hasNext()) {
                            foundProfile = true;
                        }
                    }
                } catch (Exception e) {
                    AFLog.d("find profile for cal E11 timout error-" + e.getStackTrace()[0]);

                } finally {
                    if (dbConnect != null) {
                        dbConnect.closeConnection();
                    }
                }
            }

            //profile not found
            if (!foundProfile) {
                appInstance.setFinish(true);
            }

            //build Response Error
            if (requestType.equals(ERequestType.USAGE_MONITORING)) {
                UsageMonitoringService usageMonitoringService = new UsageMonitoringService(appInstance);
                usageMonitoringService.buildResponseUsageMonitoring(false);
            } else if (requestType.equals(ERequestType.E11_TIMEOUT)) {
                E11TimoutService e11TimoutService = new E11TimoutService(appInstance);
                e11TimoutService.buildRecurringTimout();
            } else if (requestType.equals(ERequestType.GyRAR)) {
                GyRARService gyRARRequest = new GyRARService(appInstance);
                gyRARRequest.buildResponseGyRAR(false);
            } else if (requestType.equals(ERequestType.REFUND_MANAGEMENT)) {
                RefundManagementService refundTransactionService = new RefundManagementService(appInstance);
                refundTransactionService.buildResponseRefundManagement(false);
            }

        } catch (Exception e) {
            AFLog.d("Build Response Error(500) ... fail!! ,requestType:" + requestType);
            appInstance.setFinish(true);
        }


    }


}
