package phoebe.eqx.pcef.enums;

public enum EError {

    //Usage Monitoring
    USAGE_MONITORING_EXTRACT_ERROR("500000", "USAGE_MONITORING_EXTRACT_ERROR"),
    USAGE_MONITORING_MISSING_PARAMETER("500001", "USAGE_MONITORING_MISSING_PARAMETER"),
    USAGE_MONITORING_WRONG_FORMAT("500002", "USAGE_MONITORING_WRONG_FORMAT"),
    USAGE_MONITORING_BUILD_RESPONSE_ERROR("500003", "USAGE_MONITORING_BUILD_RESPONSE_ERROR"),

    //Refund Management
    REFUND_MANAGEMENT_EXTRACT_ERROR("500100", "REFUND_MANAGEMENT_EXTRACT_ERROR"),
    REFUND_MANAGEMENT_MISSING_PARAMETER("500101", "REFUND_MANAGEMENT_MISSING_PARAMETER"),
    REFUND_MANAGEMENT_WRONG_FORMAT("500102", "REFUND_MANAGEMENT_WRONG_FORMAT"),

    //GyRAR
    GYRAR_EXTRACT_ERROR("500200", "GYRAR_EXTRACT_ERROR"),
    GYRAR_MISSING_PARAMETER("500201", "GYRAR_MISSING_PARAMETER"),
    GYRAR_WRONG_FORMAT("500202", "GYRAR_WRONG_FORMAT"),

    //Usage Monitoring Start
    USAGE_MONITORING_START_BUILD_REQUEST_ERROR("500300", "USAGE_MONITORING_START_BUILD_REQUEST_ERROR"),
    USAGE_MONITORING_START_RESPONSE_TIMEOUT("500301", "USAGE_MONITORING_START_RESPONSE_TIMEOUT"),
    USAGE_MONITORING_START_RESPONSE_EQUINOX_ERROR("500302", "USAGE_MONITORING_START_RESPONSE_EQUINOX_ERROR"),
    USAGE_MONITORING_START_RESPONSE_EXTRACT_ERROR("500303", "USAGE_MONITORING_START_RESPONSE_EXTRACT_ERROR"),
    USAGE_MONITORING_START_RESPONSE_MISSING_PARAMETER("500304", "USAGE_MONITORING_START_RESPONSE_MISSING_PARAMETER"),
    USAGE_MONITORING_START_RESPONSE_WRONG_FORMAT("500305", "USAGE_MONITORING_START_RESPONSE_WRONG_FORMAT"),
    USAGE_MONITORING_START_RESPONSE_ERROR("500306", "USAGE_MONITORING_START_RESPONSE_ERROR"),
    USAGE_MONITORING_START_RESPONSE_NOT_IMPLEMENT("500307", "USAGE_MONITORING_START_RESPONSE_NOT_IMPLEMENT"),

    //Usage Monitoring Update
    USAGE_MONITORING_UPDATE_BUILD_REQUEST_ERROR("500400", "USAGE_MONITORING_UPDATE_BUILD_REQUEST_ERROR"),
    USAGE_MONITORING_UPDATE_RESPONSE_TIMEOUT("500401", "USAGE_MONITORING_UPDATE_RESPONSE_TIMEOUT"),
    USAGE_MONITORING_UPDATE_RESPONSE_EQUINOX_ERROR("500402", "USAGE_MONITORING_UPDATE_RESPONSE_EQUINOX_ERROR"),
    USAGE_MONITORING_UPDATE_RESPONSE_EXTRACT_ERROR("500403", "USAGE_MONITORING_UPDATE_RESPONSE_EXTRACT_ERROR"),
    USAGE_MONITORING_UPDATE_RESPONSE_MISSING_PARAMETER("500404", "USAGE_MONITORING_UPDATE_RESPONSE_MISSING_PARAMETER"),
    USAGE_MONITORING_UPDATE_RESPONSE_WRONG_FORMAT("500405", "USAGE_MONITORING_UPDATE_RESPONSE_WRONG_FORMAT"),
    USAGE_MONITORING_UPDATE_RESPONSE_ERROR("500406", "USAGE_MONITORING_UPDATE_RESPONSE_ERROR"),
    USAGE_MONITORING_UPDATE_RESPONSE_NOT_IMPLEMENT("500407", "USAGE_MONITORING_UPDATE_RESPONSE_NOT_IMPLEMENT"),

    //Usage Monitoring Stop
    USAGE_MONITORING_STOP_BUILD_REQUEST_ERROR("500500", "USAGE_MONITORING_STOP_BUILD_REQUEST_ERROR"),
    USAGE_MONITORING_STOP_RESPONSE_TIMEOUT("500501", "USAGE_MONITORING_STOP_RESPONSE_TIMEOUT"),
    USAGE_MONITORING_STOP_RESPONSE_EQUINOX_ERROR("500502", "USAGE_MONITORING_STOP_RESPONSE_EQUINOX_ERROR"),
    USAGE_MONITORING_STOP_RESPONSE_EXTRACT_ERROR("500503", "USAGE_MONITORING_STOP_RESPONSE_EXTRACT_ERROR"),
    USAGE_MONITORING_STOP_RESPONSE_MISSING_PARAMETER("500504", "USAGE_MONITORING_STOP_RESPONSE_MISSING_PARAMETER"),
    USAGE_MONITORING_STOP_RESPONSE_WRONG_FORMAT("500505", "USAGE_MONITORING_STOP_RESPONSE_WRONG_FORMAT"),
    USAGE_MONITORING_STOP_RESPONSE_NOT_IMPLEMENT("500506", "USAGE_MONITORING_STOP_RESPONSE_NOT_IMPLEMENT"),

    //Refund Transaction
    REFUND_TRANSACTION_BUILD_REQUEST_ERROR("500600", "REFUND_TRANSACTION_BUILD_REQUEST_ERROR"),
    REFUND_TRANSACTION_RESPONSE_TIMEOUT("500601", "REFUND_TRANSACTION_RESPONSE_TIMEOUT"),
    REFUND_TRANSACTION_RESPONSE_EQUINOX_ERROR("500602", "REFUND_TRANSACTION_RESPONSE_EQUINOX_ERROR"),
    REFUND_TRANSACTION_RESPONSE_EXTRACT_ERROR("500603", "REFUND_TRANSACTION_RESPONSE_EXTRACT_ERROR"),
    REFUND_TRANSACTION_RESPONSE_MISSING_PARAMETER("500604", "REFUND_TRANSACTION_RESPONSE_MISSING_PARAMETER"),
    REFUND_TRANSACTION_RESPONSE_WRONG_FORMAT("500605", "REFUND_TRANSACTION_RESPONSE_WRONG_FORMAT"),
    REFUND_TRANSACTION_RESPONSE_ERROR("500606", "REFUND_TRANSACTION_RESPONSE_ERROR"),

    //Get Product
    GET_PRODUCT_BUILD_REQUEST_ERROR("500700", "GET_PRODUCT_BUILD_REQUEST_ERROR"),
    GET_PRODUCT_RESPONSE_TIMEOUT("500701", "GET_PRODUCT_RESPONSE_TIMEOUT"),
    GET_PRODUCT_RESPONSE_EQUINOX_ERROR("500702", "GET_PRODUCT_RESPONSE_EQUINOX_ERROR"),
    GET_PRODUCT_RESPONSE_EXTRACT_ERROR("500703", "GET_PRODUCT_RESPONSE_EXTRACT_ERROR"),
    GET_PRODUCT_RESPONSE_MISSING_PARAMETER("500704", "GET_PRODUCT_RESPONSE_MISSING_PARAMETER"),
    GET_PRODUCT_RESPONSE_WRONG_FORMAT("500705", "GET_PRODUCT_RESPONSE_WRONG_FORMAT"),
    GET_PRODUCT_RESPONSE_DATA_NOT_FOUND("500706", "GET_PRODUCT_RESPONSE_DATA_NOT_FOUND"),
    GET_PRODUCT_RESPONSE_INVALID_DATA("500707", "GET_PRODUCT_RESPONSE_INVALID_DATA"),
    GET_PRODUCT_RESPONSE_UNAUTHORIZED("500708", "GET_PRODUCT_RESPONSE_UNAUTHORIZED"),
    GET_PRODUCT_RESPONSE_FORBIDDEN("500709", "GET_PRODUCT_RESPONSE_FORBIDDEN"),
    GET_PRODUCT_RESPONSE_NOT_FOUND("500710", "GET_PRODUCT_RESPONSE_NOT_FOUND"),
    GET_PRODUCT_RESPONSE_ERROR("500711", "GET_PRODUCT_RESPONSE_FORBIDDEN"),

    //Other
    Other("999999", "Other");


    private String code;
    private String desc;

    EError(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
