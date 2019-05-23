package phoebe.eqx.pcef.enums;

public enum EStatusResponse {

    SUCCESS("200", "Success"),
    FAIL("500", "Error");
    private String code;
    private String description;

    EStatusResponse(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

}
