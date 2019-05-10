package phoebe.eqx.pcef.states.abs;

import phoebe.eqx.pcef.enums.state.EState;
import phoebe.eqx.pcef.instance.AppInstance;


public abstract class State {
    protected AppInstance appInstance;

    private Level level;

    public State(AppInstance appInstance, Level level) {
        this.appInstance = appInstance;
        this.level = level;
    }

    abstract public void dispatch();


    protected EState getWorkState() {
        EState result = null;
        switch (level) {
            case L1:
                result = appInstance.getStateL2();
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
                result = appInstance.getStateL1();
                break;
            case L2:
                result = appInstance.getStateL2();
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

    private void setStateL1(EState state) {
        appInstance.setStateL1(state);
    }

    private void setStateL2(EState state) {
        appInstance.setStateL2(state);
    }

    public AppInstance getAppInstance() {
        return appInstance;
    }

    public enum Level {
        L1,
        L2,
    }


}
