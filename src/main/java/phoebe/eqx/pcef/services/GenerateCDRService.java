package phoebe.eqx.pcef.services;

import ec02.af.abstracts.AbstractAF;
import ec02.af.utils.AFLog;
import phoebe.eqx.pcef.core.cdr.Ctudr;
import phoebe.eqx.pcef.core.cdr.Opudr;
import phoebe.eqx.pcef.core.model.Transaction;
import phoebe.eqx.pcef.enums.EStatusLifeCycle;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.message.parser.req.RefundManagementRequest;
import phoebe.eqx.pcef.utils.PCEFUtils;
import phoebe.eqx.pcef.utils.WriteLog;

import java.util.Date;
import java.util.List;

public class GenerateCDRService {

    public static final String STATUS_CANCELED = "Canceled";
    public static final String STATUS_AlREADY_REFUND = "AlreadyRefund";


    public void buildCDRCharging(List<Transaction> transactions, AbstractAF abstractAF) {
        try {
            for (Transaction transaction : transactions) {
                Ctudr ctudr = new Ctudr();
                ctudr.setCid(generateCID());
                ctudr.setTid(transaction.getTid());
                ctudr.setRtid(transaction.getRtid());
                ctudr.setActualtime(transaction.getActualTime());
                ctudr.setApp(transaction.getApp());
                ctudr.setClientid(transaction.getClientId());
                ctudr.setUsertype(transaction.getUserType());
                ctudr.setUservalue(transaction.getUserValue());
                ctudr.setCounterid(transaction.getCounterId());
                ctudr.setMonitoringkey(transaction.getMonitoringKey());
                ctudr.setResourceid(transaction.getResourceId());
                ctudr.setResourcename(transaction.getResourceName());
                ctudr.setUnittype("unit");
                ctudr.setUsedunit("1");
                ctudr.setStatus(EStatusLifeCycle.Done.getName());
                ctudr.setHostname(PCEFUtils.HOST_NAME);

                Opudr opudr = new Opudr();
                opudr.setCtudr(ctudr);

                String cdrStr = PCEFUtils.generateCdr(opudr);
                AFLog.d("CDR STR :" + cdrStr);
                WriteLog.writeCDRCharging(abstractAF, cdrStr);
            }

        } catch (Exception e) {
            AFLog.d("Generate CDR Error .." + e.getStackTrace()[0]);
        }
    }

    public void buildCDRRefund(String status, AppInstance appInstance) {

        try {
            Transaction transaction = appInstance.getMyContext().getPcefInstance().getTransaction();
            RefundManagementRequest refundManagementRequest = appInstance.getMyContext().getPcefInstance().getRefundManagementRequest();
            Ctudr ctudr = new Ctudr();
            ctudr.setCid(generateCID());
            ctudr.setTid(refundManagementRequest.getTid());
            ctudr.setRtid(refundManagementRequest.getRtid());
            ctudr.setActualtime(refundManagementRequest.getActualTime());
            ctudr.setApp(transaction.getApp());
            ctudr.setClientid(transaction.getClientId());
            ctudr.setUsertype(transaction.getUserType());
            ctudr.setUservalue(transaction.getUserValue());
            ctudr.setCounterid(transaction.getCounterId());
            ctudr.setMonitoringkey(transaction.getMonitoringKey());
            ctudr.setResourceid(transaction.getResourceId());
            ctudr.setResourcename(transaction.getResourceName());
            ctudr.setUnittype("unit");
            ctudr.setUsedunit("1");
            ctudr.setStatus(status);
            ctudr.setHostname(PCEFUtils.HOST_NAME);

            Opudr opudr = new Opudr();
            opudr.setCtudr(ctudr);

            String cdrStr = PCEFUtils.generateCdr(opudr);
            AFLog.d("CDR STR :" + cdrStr);
            WriteLog.writeCDRRefund(appInstance.getAbstractAF(), cdrStr);

        } catch (Exception e) {
            AFLog.d("Generate CDR Error .." + e.getStackTrace()[0]);
        }
    }


    private String generateCID() {
        return "CDR" + PCEFUtils.cdrDateFormat.format(new Date());
    }


}
