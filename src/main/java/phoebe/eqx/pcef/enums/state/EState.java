package phoebe.eqx.pcef.enums.state;

public enum EState {
    BEGIN,
    WAIT_A,
    W_QUERY_DB,

    W_USAGE_MONITORING_START,
    W_USAGE_MONITORING_UPDATE,

    W_USAGE_MONITORING_STOP,
    W_REFUND_TRANSACTION,
    W_GET_RESOURCE_ID,
    END
}
