package phoebe.eqx.pcef.enums.stats;

public enum EStatMode {
    SUCCESS("[SUCCESS]"),
    ERROR("[ERROR]"),
    TIMEOUT("[TIMEOUT]"),
    BAD("[BAD]"),
    UNDER_PROCESSING("[UNDER_PROCESSING]"),
    EQUINOX_ERROR("[EQUINOX_ERROR]"),
    MONETARY("[2XXXXX]"),
    COUNTER_NOT_FOUND("[COUNTER_NOT_FOUND]"),
    DOMESTIC("[DOMESTIC]"),
    ROAMING("[ROAMING]");


    private String mode;

    EStatMode(String mode) {
        this.mode = mode;
    }

    public String getMode() {
        return mode;
    }
}
