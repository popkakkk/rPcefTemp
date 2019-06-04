package phoebe.eqx.pcef.enums;

public enum Operation {
    TestOperation, WaitInterval,

    GetResourceId,

    /*---Usage Monitoring */
    QueryDBPrivateID,
    //start
    UsageMonitoringStart,
    CreateCorrection,

    //update
    UpdateCorrection,
    UsageMonitoringUpdate,
    UsageMonitoringStop,
    RefundTransaction,

    //response
    Response
}
