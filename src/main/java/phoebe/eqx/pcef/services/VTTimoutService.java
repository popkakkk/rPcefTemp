package phoebe.eqx.pcef.services;

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

        MessagePool messagePool = new MessagePool(appInstance.getAbstractAF());
        Date currentDate = new Date();
        Date appointmentDate = appInstance.getPcefInstance().getProfile().getAppointmentDate();
        String timeout = String.valueOf(appointmentDate.compareTo(currentDate));
        EquinoxRawData equinoxRawData = messagePool.recurringVTTimeout(timeout);
        appInstance.getOutList().add(equinoxRawData);
    }


}
