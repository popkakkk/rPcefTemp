package phoebe.eqx.pcef.core.exceptions;

import phoebe.eqx.pcef.enums.EError;

public class PCEFException extends Exception {


    public PCEFException() {
    }

    private String errorMsg;
    private EError error;

    public PCEFException(String errorMsg, EError error) {
        this.error = error;
        this.errorMsg = errorMsg;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public EError getError() {
        return error;
    }

    public void setError(EError error) {
        this.error = error;
    }
}
