package phoebe.eqx.pcef.services;

import ec02.af.utils.AFLog;
import ec02.data.interfaces.EquinoxRawData;
import phoebe.eqx.pcef.core.exceptions.TimeoutIntervalException;
import phoebe.eqx.pcef.enums.Operation;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.instance.Config;
import phoebe.eqx.pcef.message.builder.MessagePool;

public class E11TimoutService extends PCEFService {

    public E11TimoutService(AppInstance appInstance) {
        super(appInstance);
    }

    public void buildRecurringTimout() {
        AFLog.d("Build Recurring Timeout");
        MessagePool messagePool = new MessagePool(appInstance.getAbstractAF());
        EquinoxRawData equinoxRawData = messagePool.sentTimeout(getTimeoutFromAppointmentDate(), "response");
        appInstance.getOutList().add(equinoxRawData);
    }

    public void buildInterval() throws TimeoutIntervalException {
        AFLog.d("Build Interval Process");
        appInstance.getMyContext().setHasRequest(true);

        if (context.getIntervalRetry() >= Config.RETRY_PROCESSING) {
            AFLog.d("Interval Timeout!!!");
            throw new TimeoutIntervalException();
        }
        context.setIntervalRetry(context.getIntervalRetry() + 1);

        MessagePool messagePool = new MessagePool(appInstance.getAbstractAF());
        EquinoxRawData equinoxRawData = messagePool.sentTimeout(String.valueOf(Config.INTERVAL_PROCESSING), "request");
        context.setInterval(true);
        AFLog.d("WAIT INTERVAL :" + context.getIntervalRetry() + " TIME!!!");
        AFLog.d("set Interval :" + context.isInterval());
        appInstance.getOutList().add(equinoxRawData);
    }

}
