package phoebe.eqx.pcef.utils;

public enum DBResult {
    SUCCESS("20000", "Success"),
    ERROR("50000", "Error");


    private String code;
    private String desc;

    DBResult(String code, String desc) {
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
