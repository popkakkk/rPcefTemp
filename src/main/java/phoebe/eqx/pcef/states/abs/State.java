package phoebe.eqx.pcef.states.abs;

import phoebe.eqx.pcef.enums.state.EState;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.instance.context.RequestContext;


public abstract class State {
    protected AppInstance appInstance;
    protected RequestContext context;


    private Level level;

    public State(AppInstance appInstance, Level level) {
        this.appInstance = appInstance;
        this.level = level;
        this.context = appInstance.getMyContext();
    }

    abstract public void dispatch();


    protected EState getWorkState() {
        EState result = null;
        switch (level) {
            case L1:
                result = appInstance.getMyContext().getStateL2();
                break;
            case L2:
                result = appInstance.getMyContext().getStateL3();
                break;

        }
        return result;
    }

    protected void setWorkState(EState state) {
        switch (level) {
            case L1:
                setStateL2(state);
                break;
            case L2:
                setStateL3(state);
                break;
        }
    }

    protected EState getState() {
        EState result = null;
        switch (level) {
            case L1:
                result = appInstance.getMyContext().getStateL1();
                break;
            case L2:
                result = appInstance.getMyContext().getStateL2();
                break;
            case L3:
                result = appInstance.getMyContext().getStateL3();
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
            case L3:
                setStateL3(state);
                break;

        }
    }

    private void setStateL1(EState state) {
        appInstance.getMyContext().setStateL1(state);
    }

    private void setStateL2(EState state) {
        appInstance.getMyContext().setStateL2(state);
    }

    private void setStateL3(EState state) {
        appInstance.getMyContext().setStateL3(state);
    }

    public AppInstance getAppInstance() {
        return appInstance;
    }

    public enum Level {
        L1,
        L2,
        L3,
    }


}
