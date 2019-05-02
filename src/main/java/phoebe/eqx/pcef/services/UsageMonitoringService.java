package phoebe.eqx.pcef.services;

import ec02.data.interfaces.EquinoxRawData;
import phoebe.eqx.pcef.enums.Operation;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.message.req.UsageMonitroingStartRequest;

public class UsageMonitoringService extends PCEFService {
    public UsageMonitoringService(AppInstance appInstance) {
        super(appInstance);
    }

    public void buildUsageMonitoringStart() {
        try {
            Operation operation = Operation.UsageMonitoringStart;
            String invokeId = "umstart_";


            UsageMonitroingStartRequest usageMonitroingStartRequest = new UsageMonitroingStartRequest();
            usageMonitroingStartRequest.setCommand("usageMonitoringStart");


            EquinoxRawData equinoxRawData = msgPool.getUsageMonitoringStartRequest(usageMonitroingStartRequest, invokeId);
            invokeExternal(equinoxRawData, operation, msgPool.getRequestObj());

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
