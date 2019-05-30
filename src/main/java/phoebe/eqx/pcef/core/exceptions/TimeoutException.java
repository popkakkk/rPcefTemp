package phoebe.eqx.pcef.core.exceptions;

import phoebe.eqx.pcef.enums.EError;

public class TimeoutException extends PCEFException {
    public TimeoutException() {

    }

    public TimeoutException(String errorMsg, EError eError) {
        super(errorMsg, eError);
    }
}
