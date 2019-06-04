package phoebe.eqx.pcef.states.abs;

import com.google.gson.Gson;
import phoebe.eqx.pcef.enums.state.EState;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.services.mogodb.MongoDBConnect;

public class MongoState extends ComplexState {
    protected boolean responseSuccess;
    protected MongoDBConnect dbConnect;
    protected Gson gson = new Gson();

    public MongoState(AppInstance appInstance, Level level, MongoDBConnect mongoDBConnect) {
        super(appInstance, level);
        dbConnect = mongoDBConnect;
    }

    public void setResponseFail() {
        setState(EState.END);
        this.responseSuccess = false;
    }

    public boolean isResponseSuccess() {
        return responseSuccess;
    }

    public void setResponseSuccess() {
        setState(EState.END);
        this.responseSuccess = true;
    }


}
