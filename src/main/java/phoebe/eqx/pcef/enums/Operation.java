package phoebe.eqx.pcef.enums;

public enum Operation {
    TestOperation,

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

    //response
    Response
}
