package phoebe.eqx.pcef.states.mongodb.abs;

import phoebe.eqx.pcef.enums.EMongoState;

import java.lang.reflect.Method;


public abstract class MongoState {

    private EMongoState nextState = EMongoState.BEGIN;
    private boolean isInterval;

    public void dispatch() throws Exception {
        boolean continueWork = true;
        while (continueWork) {
            Class clazz = getClass();
            Method method = findMethod(clazz, nextState);
            method.invoke(this);
            continueWork = !EMongoState.END.equals(nextState);
        }
    }


    public Method findMethod(Class clazz, EMongoState state) throws Exception {
        for (Method method : clazz.getMethods()) {
            if (method.isAnnotationPresent(MessageMongoRecieved.class)) {
                MessageMongoRecieved messageRecieved = method.getAnnotation(MessageMongoRecieved.class);
                if (state.equals(messageRecieved.messageType())) {
                    return method;
                }
            }
        }
        throw new Exception("Can not execute method mongo for " + state);
    }

    public EMongoState getNextState() {
        return nextState;
    }

    public void setNextState(EMongoState nextState) {
        if (nextState == null) {
            this.nextState = EMongoState.END;
        }
        this.nextState = nextState;
    }
}
