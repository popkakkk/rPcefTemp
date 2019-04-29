package phoebe.eqx.pcef.enums.stats;

public enum EStatCmd {

    PCEF_RECEIVE_TEST_DATA("PCEF_RECEIVE_TEST_DATA"),
    ;

    String cmd;

    EStatCmd(String cmd) {
        this.cmd = cmd;
    }

    public String getCmd() {
        return cmd;
    }
}
