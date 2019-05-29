package phoebe.eqx.pcef.instance;

import ec02.af.abstracts.AbstractAF;
import ec02.af.utils.AFLog;
import ec02.data.interfaces.EquinoxRawData;
import phoebe.eqx.pcef.core.data.InvokeObject;
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

    //------ instance logs ------------------
    private List<SummaryLog> summaryLogs = new ArrayList<>();
    private String requestLog;
    private String responseLog;


    //------- transient ---------------------
    private transient RequestContext myContext;


    private transient ArrayList<EquinoxRawData> outList = new ArrayList<>();
    private transient boolean finish;
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


    public void patchResponse() {
        myContext.getInvokeManager().patchResponse(outList);
    }
 /*   public void setSummaryLogExternalResponse(Operation operation, Object resp) throws Exception {

        InvokeObject invokeObject = this.invokeManager.find(operation);
        SummaryLog summaryLog = findMatchSummaryLog(invokeObject.getInvokeId(), operation.name());

        if (summaryLog != null) {
            Date resTime = invokeObject.getResTime();
            summaryLog.getSummaryLogDetail().setRes(resp);
            summaryLog.getSummaryLogDetail().setResTime(PCEFUtils.dtLongFormatterMs.format(resTime));

            Date reqTime = PCEFUtils.dtLongFormatterMs.parse(summaryLog.getSummaryLogDetail().getReqTime());
            long usedTime = resTime.getTime() - reqTime.getTime();
            summaryLog.getSummaryLogDetail().setUsedTime(String.valueOf(usedTime));
        }
    }*/

    public SummaryLog findMatchSummaryLog(String invokeId, String logName) {
        for (SummaryLog summaryLog : summaryLogs) {
            if (summaryLog.getInvokeId().equals(invokeId)) {
                if (summaryLog.getLogName().equals(logName)) {
                    return summaryLog;
                }
            }
        }
        AFLog.d("SummaryLog list Don't match invoke");
        return null;
    }

    public String getSummaryLogStr() {
        StringBuilder builder = new StringBuilder().append("{");
        for (SummaryLog summaryLog : summaryLogs) {
            HashMap<String, SummaryLogDetail> logNameMapDetils = new HashMap<>();
            logNameMapDetils.put(summaryLog.getLogName(), summaryLog.getSummaryLogDetail());
            builder.append(PCEFUtils.gsonToJson(logNameMapDetils));
        }
        return builder.append("}").toString();
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

    public List<SummaryLog> getSummaryLogs() {
        return summaryLogs;
    }

    public void setSummaryLogs(List<SummaryLog> summaryLogs) {
        this.summaryLogs = summaryLogs;
    }

    public String getRequestLog() {
        return requestLog;
    }

    public void setRequestLog(String requestLog) {
        this.requestLog = requestLog;
    }

    public String getResponseLog() {
        return responseLog;
    }

    public void setResponseLog(String responseLog) {
        this.responseLog = responseLog;
    }


}
