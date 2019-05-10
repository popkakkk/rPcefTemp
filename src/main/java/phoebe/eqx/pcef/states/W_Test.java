package phoebe.eqx.pcef.states;

import phoebe.eqx.pcef.enums.state.EState;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.services.MyService;
import phoebe.eqx.pcef.states.abs.ComplexState;
import phoebe.eqx.pcef.states.abs.MessageRecieved;

public class W_Test extends ComplexState {

    public W_Test(AppInstance appInstance) {
        super(appInstance, Level.L1);
        this.setState(EState.BEGIN);
    }

    @MessageRecieved(messageType = EState.BEGIN)
    public void begin() throws Exception {
        MyService myService = new MyService(getAppInstance());
        myService.readCommandRequestTest();
        myService.buildTest();
        setWorkState(EState.WAIT_A);
    }

    @MessageRecieved(messageType = EState.WAIT_A)
    public void W_A() throws Exception {
        MyService myService = new MyService(getAppInstance());
        myService.readTest();
        myService.buildTest();
        setWorkState(EState.WAIT_A);
    }


}
