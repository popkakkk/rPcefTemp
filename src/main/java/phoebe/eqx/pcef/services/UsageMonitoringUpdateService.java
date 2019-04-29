package phoebe.eqx.pcef.services;

import phoebe.eqx.pcef.enums.Operation;
import phoebe.eqx.pcef.instance.AppInstance;

public class UsageMonitoringUpdateService extends PCEFService {

    public UsageMonitoringUpdateService(AppInstance appInstance) {
        super(appInstance);
    }

    public void buildUsageMonitoringStop() {
        try {
            Operation operation = Operation.UsageMonitoringUpdate;
        } catch (Exception e) {

        }
    }


    public void readUsageMonitoringStop() {
        try {
            Operation operation = Operation.UsageMonitoringUpdate;
        } catch (Exception e) {

        }
    }

}
