package phoebe.eqx.pcef.services;

import com.google.gson.Gson;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.message.Metering;

public class SACFService extends PCEFService {
    public SACFService(AppInstance appInstance) {
        super(appInstance);
    }

    public void readRequest() {

        try {
            String message = context.getReqMessage();
            Metering metering = new Gson().fromJson(message, Metering.class);
            appInstance.getPcefInstance().setMetering(metering);
        } catch (Exception e) {

        }


    }


    public void buildResponseSACFSuccess() {


    }

    public void buildResponseSACFFail() {


    }


}
