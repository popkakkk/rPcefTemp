package phoebe.eqx.pcef.states.abs;

import ec02.af.utils.AFLog;
import phoebe.eqx.pcef.core.exceptions.PCEFException;
import phoebe.eqx.pcef.enums.state.EState;
import phoebe.eqx.pcef.instance.AppInstance;

import java.lang.reflect.Method;

public abstract class ComplexState extends State {
    private boolean stop;

    public ComplexState(AppInstance appInstance, Level level) {
        super(appInstance, level);
    }

    private Object dispatchToWorkingState(EState subState) throws Exception {
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
                AFLog.d("Set state to " + state + " -->  BEGIN");
                state = EState.BEGIN;
            }

            try {
                dispatchToWorkingState(state); //execute state
            } catch (PCEFException e) {

            } catch (Exception e) {
                appInstance.setFinish(true);
                stop = true;
            }
            EState workState = getWorkState();
            continueWork = !this.appInstance.isHasRequest();
            if (EState.END.equals(workState)) {
                if (handler != null) {
                    handler.onEnd();
                }
                AFLog.d("stop by set stateWork .. RESPONSE");
                break;
            }
            if (stop) {
                AFLog.d("Stop...by..flag");
                break;
            }
        }

    }

    public static Method findMethod(Class clazz, EState state) throws Exception {
        for (Method method : clazz.getMethods()) {
            if (method.isAnnotationPresent(MessageRecieved.class)) {
                MessageRecieved messageRecieved = method.getAnnotation(MessageRecieved.class);
                if (state.equals(messageRecieved.messageType())) {
                    return method;
                }
            }
        }
        throw new Exception("Can not execute method for " + state);
    }

    public boolean isStop() {
        return stop;
    }

    public void setStop(boolean stop) {
        this.stop = stop;
    }


}
