package phoebe.eqx.pcef.instance;

import ec02.af.abstracts.AbstractAF;
import ec02.af.utils.AFLog;
import ec02.data.interfaces.EquinoxRawData;
import phoebe.eqx.pcef.core.data.InvokeObject;
import phoebe.eqx.pcef.core.exceptions.PCEFException;
import phoebe.eqx.pcef.core.logs.summary.SummaryLog;
import phoebe.eqx.pcef.core.logs.summary.SummaryLogDetail;
import phoebe.eqx.pcef.enums.EEvent;
import phoebe.eqx.pcef.enums.ERequestType;
import phoebe.eqx.pcef.enums.state.EState;
import phoebe.eqx.pcef.enums.Operation;
import phoebe.eqx.pcef.instance.context.RequestContext;
import phoebe.eqx.pcef.message.parser.req.UsageMonitoringRequest;
import phoebe.eqx.pcef.utils.PCEFUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class AppInstance {


    List<RequestContext> requestContexts = new ArrayList<>();



    //------- transient ---------------------
    private transient RequestContext myContext;


    private transient ArrayList<EquinoxRawData> outList = new ArrayList<>();
    private transient boolean finish; //summary and ret 10
    private transient AbstractAF abstractAF;


    public RequestContext findCompleteContextListMatchResponse(EquinoxRawData rawData, String ret) throws Exception {
        for (RequestContext requestContext : requestContexts) {
            EEvent event = PCEFUtils.getEventByRet(ret);
            if (requestContext.getInvokeManager().putRawData(rawData, event)) {
                return requestContext;
            }
        }
        throw new Exception("No invoke response match context");
    }

    public void removeRequestContext() {
        AFLog.d("remove context");
        requestContexts.remove(myContext);
    }


    public void patchResponse() {
        myContext.getInvokeManager().patchResponse(outList);

    }


    public RequestContext getRequestContextTimeout() {
        RequestContext requestContext = null;
        try {
            //find min timeoutDate
            for (RequestContext context : requestContexts) {

                if (requestContext == null) {
                    requestContext = context;
                } else {
                    if (context.getTimeoutDate().before(requestContext.getTimeoutDate())) {
                        requestContext = context;
                    }
                }
            }
            AFLog.d("Get min timeout date" + requestContext.getTimeoutDate() + ",request context =" + requestContext.getRequestType());
        } catch (Exception e) {
            AFLog.d("Get Request Context Timeout error:" + e.getStackTrace()[0]);
            throw e;
        }
        return requestContext;
    }




    public RequestContext getMyContext() {
        return myContext;
    }

    public void setMyContext(RequestContext myContext) {
        this.myContext = myContext;
    }

    public List<RequestContext> getRequestContexts() {
        return requestContexts;
    }

    public void setRequestContexts(List<RequestContext> requestContexts) {
        this.requestContexts = requestContexts;
    }

    public ArrayList<EquinoxRawData> getOutList() {
        return outList;
    }

    public void setOutList(ArrayList<EquinoxRawData> outList) {
        this.outList = outList;
    }

    public AbstractAF getAbstractAF() {
        return abstractAF;
    }

    public void setAbstractAF(AbstractAF abstractAF) {
        this.abstractAF = abstractAF;
    }

    public boolean isFinish() {
        return finish;
    }

    public void setFinish(boolean finish) {
        this.finish = finish;
    }




}
