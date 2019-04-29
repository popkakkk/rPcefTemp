package phoebe.eqx.pcef.instance;

import ec02.af.abstracts.AbstractAF;
import ec02.af.utils.AFLog;
import ec02.data.interfaces.EquinoxRawData;
import phoebe.eqx.pcef.core.context.InvokeManager;
import phoebe.eqx.pcef.core.context.RequestContext;
import phoebe.eqx.pcef.core.data.InvokeObject;
import phoebe.eqx.pcef.core.logs.summary.SummaryLog;
import phoebe.eqx.pcef.core.logs.summary.SummaryLogDetail;
import phoebe.eqx.pcef.enums.Operation;
import phoebe.eqx.pcef.utils.PCEFUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class AppInstance {

    //----- instance data -------------------
    private PCEFInstance pcefInstance;

    //------ context ------------------------
    private RequestContext requestContext;
    private InvokeManager invokeManager;

    //------ instance logs ------------------
    private List<SummaryLog> summaryLogs;
    private String requestLog;
    private String responseLog;

    //------- transient ---------------------
    private transient ArrayList<EquinoxRawData> outList = new ArrayList<>();
    private transient boolean finish;
    private transient AbstractAF abstractAF;

    public synchronized void create() {
        this.pcefInstance = new PCEFInstance();
        this.requestContext = new RequestContext();
        this.invokeManager = new InvokeManager();
        this.summaryLogs = new ArrayList<>();
    }

    public void setSummaryLogExternalResponse(Operation operation, Object resp) throws Exception {

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
    }

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

    public AbstractAF getAbstractAF() {
        return abstractAF;
    }

    public void setAbstractAF(AbstractAF abstractAF) {
        this.abstractAF = abstractAF;
    }

    public RequestContext getRequestContext() {
        return requestContext;
    }

    public void setRequestContext(RequestContext requestContext) {
        this.requestContext = requestContext;
    }


    public InvokeManager getInvokeManager() {
        return invokeManager;
    }

    public void setInvokeManager(InvokeManager invokeManager) {
        this.invokeManager = invokeManager;
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

    public PCEFInstance getPcefInstance() {
        return pcefInstance;
    }

    public void setPcefInstance(PCEFInstance pcefInstance) {
        this.pcefInstance = pcefInstance;
    }

    public ArrayList<EquinoxRawData> getOutList() {
        return outList;
    }

    public void setOutList(ArrayList<EquinoxRawData> outList) {
        this.outList = outList;
    }

    public void patchResponse() {
        invokeManager.patchResponse(outList);
    }
}
