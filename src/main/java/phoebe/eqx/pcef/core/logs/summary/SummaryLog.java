package phoebe.eqx.pcef.core.logs.summary;

import phoebe.eqx.pcef.enums.Operation;
import phoebe.eqx.pcef.utils.PCEFUtils;

import java.util.Date;

public class SummaryLog {
    private String logName; //get from operation
    private String invokeId;
    private SummaryLogDetail summaryLogDetail;

    public SummaryLog(String logName, String invokeId, Date reqTime, Object req) {
        this.logName = logName;
        this.invokeId = invokeId;
        this.summaryLogDetail = new SummaryLogDetail(invokeId, PCEFUtils.dtLongFormatterMs.format(reqTime), req);
    }

    public static Object getSummaryLogResponse(Operation operation, Object responseObj) {
        Object object = "";
        if (Operation.TestOperation.equals(operation)) {
            //get summary log response
            object = responseObj;
        }
        return object;
    }

    public static Object getSummaryLogRequest(Operation operation, Object responseObj) {
        Object object = "";
        if (Operation.TestOperation.equals(operation)) {
            //get summary log request
            object = responseObj;
        }
        return object;
    }


    public String getInvokeId() {
        return invokeId;
    }

    public void setInvokeId(String invokeId) {
        this.invokeId = invokeId;
    }

    public String getLogName() {
        return logName;
    }

    public void setLogName(String logName) {
        this.logName = logName;
    }

    public SummaryLogDetail getSummaryLogDetail() {
        return summaryLogDetail;
    }

    public void setSummaryLogDetail(SummaryLogDetail summaryLogDetail) {
        this.summaryLogDetail = summaryLogDetail;
    }
}
