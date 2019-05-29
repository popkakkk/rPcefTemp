package phoebe.eqx.pcef.enums.stats;

public enum EStatMode {
    SUCCESS("[SUCCESS]"),
    ERROR("[ERROR]"),
    TIMEOUT("[TIMEOUT]"),
    BAD("[BAD]"),
    EXTRACT_ERROR("[EXTRACT_ERROR]"),
    MISSING_PARAMETER("[MISSING_PARAMETER]"),
    WRONG_FORMAT("[WRONG_FORMAT]"),
    NOT_IMPLEMENT("[NOT_IMPLEMENT]"),
    PARTIAL_SUCCESS("[PARTIAL_SUCCESS]"),
    EQUINOX_ERROR("[EQUINOX_ERROR]");


    private String mode;

    EStatMode(String mode) {
        this.mode = mode;
    }

    public String getMode() {
        return mode;
    }
}
