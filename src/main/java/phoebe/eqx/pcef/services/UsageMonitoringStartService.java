package phoebe.eqx.pcef.services;

import ec02.data.interfaces.EquinoxRawData;
import phoebe.eqx.pcef.enums.Operation;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.utils.MockData;

public class UsageMonitoringStartService extends PCEFService {

    public UsageMonitoringStartService(AppInstance appInstance) {
        super(appInstance);
    }

    public void buildUsageMonitoringStart() {
        try {
            Operation operation = Operation.UsageMonitoringStart;
            String invokeId = "umstart_";

            /* mock */
            String data = MockData.startRequest;

            EquinoxRawData equinoxRawData = msgPool.getUsageMonitringStartRequest(data, invokeId);
            invokeExternal(equinoxRawData, Operation.TestOperation, msgPool.getRequestObj());


        } catch (Exception e) {

        }
    }


    public void readUsageMonitoringStart() {
        try {
            Operation operation = Operation.UsageMonitoringStart;
        } catch (Exception e) {

        }
    }

}
