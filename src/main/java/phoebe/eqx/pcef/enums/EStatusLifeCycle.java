package phoebe.eqx.pcef.enums;

public enum EStatusLifeCycle {

    Waiting("Waiting"),
    Done("Done"),
    Processing(""),
    Canceled("Canceled"),
    Completed("Completed"),
    WaitingRefund("WaitingRefund"),
    AlreadyRefund("AlreadyRefund");

    private String status;

    EStatusLifeCycle(String status) {
        this.status = status;
    }

    public String getName() {
        return status;
    }

}
