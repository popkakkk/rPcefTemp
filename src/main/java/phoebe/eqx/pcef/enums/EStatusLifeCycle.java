package phoebe.eqx.pcef.enums;

public enum EStatusLifeCycle {

    Waiting("Waiting"),
    Done("Done"),
    Completed("Completed");


    private String status;

    EStatusLifeCycle(String status) {
        this.status = status;
    }

    public String getName() {
        return status;
    }

}
