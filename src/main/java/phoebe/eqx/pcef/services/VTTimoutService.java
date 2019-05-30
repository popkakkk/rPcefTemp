package phoebe.eqx.pcef.services;

import ec02.af.utils.AFLog;
import ec02.data.interfaces.EquinoxRawData;
import phoebe.eqx.pcef.enums.state.EMongoState;
import phoebe.eqx.pcef.enums.state.EState;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.message.builder.MessagePool;

import java.util.Date;

public class VTTimoutService extends PCEFService {

    public VTTimoutService(AppInstance appInstance) {
        super(appInstance);
    }

    public void buildRecurringTimout() {
        AFLog.d("Build Recurring Timeout");
        MessagePool messagePool = new MessagePool(appInstance.getAbstractAF());
        EquinoxRawData equinoxRawData = messagePool.recurringVTTimeout(getTimeoutFromAppointmentDate());
        appInstance.getOutList().add(equinoxRawData);
    }
}
