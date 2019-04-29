package phoebe.eqx.pcef.main;

import ec02.af.abstracts.AbstractAF;
import ec02.af.utils.AFLog;
import ec02.data.interfaces.*;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.instance.Config;

import java.util.ArrayList;
import java.util.List;

public class PCEFMain extends AbstractAF {
    @Override
    public ECDialogue actionProcess(EquinoxPropertiesAF equinoxPropertiesAF, List<EquinoxRawData> eqxRawDatas, InstanceData instanceData) {
        AppInstance appInstance = EQX4Wrapper.extractInstance(String.valueOf(instanceData));
        AFLog.d("## actionProcess || extractInstance || Finish...");

        ECDialogue ecDialogue = EQX4Wrapper.callActionProcess(equinoxPropertiesAF, this, (ArrayList<EquinoxRawData>) eqxRawDatas, appInstance);
        AFLog.d("## actionProcess || callActionProcess || Finish...");

        String instance = EQX4Wrapper.composeInstance(appInstance);
        AFLog.d("## actionProcess || composeInstance || Finish...");

        this.getEquinoxUtils().setInstanceMessage(instance);
        return ecDialogue;
    }

    @Override
    public boolean verifyAFConfiguration(String s) {
        boolean verify = Config.verify(this);
        if (verify) {
            Config.loadConfiguration(this);
        }
        return verify;
    }

    @Override
    public StdCDRData initializedCallDetailRecord() {
        return null;
    }

    @Override
    public StdEDRFactory initializedEventDetailRecord() {
        return null;
    }
}
