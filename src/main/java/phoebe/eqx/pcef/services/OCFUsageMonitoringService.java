package phoebe.eqx.pcef.services;

import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import ec02.data.interfaces.EquinoxRawData;
import phoebe.eqx.pcef.core.data.OCFUsageMonitoring;
import phoebe.eqx.pcef.core.data.Resource;
import phoebe.eqx.pcef.core.exceptions.ResponseErrorException;
import phoebe.eqx.pcef.core.exceptions.TimeoutException;
import phoebe.eqx.pcef.enums.Operation;
import phoebe.eqx.pcef.instance.AppInstance;
import phoebe.eqx.pcef.instance.PCEFInstance;
import phoebe.eqx.pcef.message.builder.MessagePool;
import phoebe.eqx.pcef.message.builder.req.UsageMonitoringStartRequest;
import phoebe.eqx.pcef.message.parser.req.UsageMonitoringRequest;
import phoebe.eqx.pcef.model.Transaction;
import phoebe.eqx.pcef.utils.ValidateMessage;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OCFUsageMonitoringService extends PCEFService {
    public OCFUsageMonitoringService(AppInstance appInstance) {
        super(appInstance);
    }


    public boolean receiveQuotaAndPolicy() {
        boolean receive = false;
        List<Resource> resourceList = appInstance.getPcefInstance().getOcfUsageMonitoring().getResources();

        for (Resource resource : resourceList) {


        }

        return true;
    }

    public void buildUsageMonitoringStart(List<Resource> resourceList) {
        try {
            Transaction transaction = appInstance.getPcefInstance().getTransaction();

            Operation operation = Operation.UsageMonitoringStart;
            String invokeId = "umStart_";

            UsageMonitoringStartRequest umStartRequest = new UsageMonitoringStartRequest();
            umStartRequest.setCommand("usageMonitoringStart");
            umStartRequest.setSessionId(transaction.getSessionId());
//            umStartRequest.setTid();
//            umStartRequest.setRequestNumber();
            umStartRequest.setUserType(transaction.getUserType());
            umStartRequest.setUserValue(transaction.getUserValue());


            Resource resourceOfTransaction = new Resource();
            resourceOfTransaction.setResourceId(transaction.getResourceId());
            resourceOfTransaction.setResourceName(transaction.getResourceName());
            resourceOfTransaction.setRtid(transaction.getRtid());
            resourceList.add(resourceOfTransaction);

            umStartRequest.setResources((ArrayList<Resource>) resourceList);

            MessagePool messagePool = new MessagePool(abstractAF);
            EquinoxRawData equinoxRawData = messagePool.getUsageMonitoringStartRequest(umStartRequest, invokeId);
            invokeExternal(equinoxRawData, operation, messagePool.getRequestObj());

        } catch (Exception e) {

        }
    }

    public void buildUsageMonitoringUpdate() {
        try {


        } catch (Exception e) {

        }
    }

    public void readUsageMonitoringStart() {

        try {
            Operation operation = Operation.UsageMonitoringStart;

            //extract
            OCFUsageMonitoring OCFUsageMonitoring = (OCFUsageMonitoring) extractResponse(operation);
            this.appInstance.getPcefInstance().setOcfUsageMonitoring(OCFUsageMonitoring);

            //validate
            ValidateMessage.validateTestData();

            //increase stat success
//            PCEFUtils.increaseStatistic(abstractAF, EStatMode.SUCCESS, EStatCmd.PCEF_RECEIVE_TEST_DATA);

            //summarylog res
//            appInstance.setSummaryLogExternalResponse(Operation.TestOperation, SummaryLog.getSummaryLogResponse(Operation.TestOperation, testResponseData));
        } catch (TimeoutException e) {
            // handle time out
        } catch (ResponseErrorException e) {
            // handle ret error
        } catch (Exception e) {
            //increase stat fail
            //summarylog fail
            // read fail
        }

    }


}
