package phoebe.eqx.pcef.services;

import com.google.gson.Gson;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.message.req.MeteringRequest;

public class SACFService extends PCEFService {
    public SACFService(AppInstance appInstance) {
        super(appInstance);
    }

    public void readRequest() {

        try {
            String message = context.getReqMessage();
            MeteringRequest meteringRequest = new Gson().fromJson(message, MeteringRequest.class);
            appInstance.getPcefInstance().setMeteringRequest(meteringRequest);
        } catch (Exception e) {

        }


    }


    public void buildResponseSACFSuccess() {


    }


}
