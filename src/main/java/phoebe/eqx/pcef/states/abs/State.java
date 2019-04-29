package phoebe.eqx.pcef.states.abs;

import phoebe.eqx.pcef.core.context.RequestContext;
import phoebe.eqx.pcef.enums.EState;
import phoebe.eqx.pcef.instance.AppInstance;

import java.lang.reflect.Method;


public abstract class State {
    protected AppInstance appInstance;
    protected RequestContext context;
    private Level level;

    public State(AppInstance appInstance, Level level) {
        this.appInstance = appInstance;
        this.context = appInstance.getRequestContext();
        this.level = level;
    }

    abstract public void dispatch();

    public Method findMethod(Class clazz, EState state) throws Exception {
        for (Method method : clazz.getMethods()) {
            if (method.isAnnotationPresent(MessageRecieved.class)) {
                MessageRecieved messageRecieved = method.getAnnotation(MessageRecieved.class);
                if (state.equals(messageRecieved.messageType())) {
                    return method;
                }
            }
        }
        throw new Exception("Can not find method for " + state);
    }

    protected EState getWorkState() {
        EState result = null;
        switch (level) {
            case L1:
                result = context.getStateL2();
                break;

        }
        return result;
    }

    protected void setWorkState(EState state) {
        switch (level) {
            case L1:
                setStateL2(state);
                break;
        }
    }

    protected EState getState() {
        EState result = null;
        switch (level) {
            case L1:
                result = context.getStateL1();
                break;
            case L2:
                result = context.getStateL2();
                break;

        }
        return result;
    }

    protected void setState(EState state) {
        switch (level) {
            case L1:
                setStateL1(state);
                break;
            case L2:
                setStateL2(state);
                break;

        }
    }

    public void setStateL1(EState state) {
        context.setStateL1(state);
    }

    public void setStateL2(EState state) {
        context.setStateL2(state);
    }

    public AppInstance getAppInstance() {
        return appInstance;
    }

    public enum Level {
        L1,
        L2,
    }


}
