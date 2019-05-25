package phoebe.eqx.pcef.states.mongodb.abs;

import com.google.gson.Gson;
import phoebe.eqx.pcef.enums.state.EMongoState;
import phoebe.eqx.pcef.enums.state.EState;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.services.mogodb.MongoDBConnect;

import java.lang.reflect.Method;


public abstract class MongoState {

    private EMongoState nextState = EMongoState.BEGIN;
    private EState pcefState;
    protected AppInstance appInstance;
    protected MongoDBConnect dbConnect;
    protected Gson gson = new Gson();

    public MongoState(AppInstance appInstance, MongoDBConnect dbConnect) {
        this.appInstance = appInstance;
        this.dbConnect = dbConnect;
    }

    public void dispatch() throws Exception {
        boolean continueWork = true;
        while (continueWork) {
            Class clazz = getClass();
            Method method = findMethod(clazz, nextState);

            EMongoState state = (EMongoState) method.invoke(this);
            nextState = (state == null) ? EMongoState.END : state;
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
        } else {
            this.nextState = nextState;
        }
    }


    public EState getPcefState() {
        return pcefState;
    }

    public void setPcefState(EState pcefState) {
        this.pcefState = pcefState;
    }


}
