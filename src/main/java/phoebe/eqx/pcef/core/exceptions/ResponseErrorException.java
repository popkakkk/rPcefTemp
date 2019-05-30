package phoebe.eqx.pcef.core.exceptions;

import phoebe.eqx.pcef.enums.EError;

public class ResponseErrorException extends PCEFException {


    public ResponseErrorException() {
    }

    public ResponseErrorException(String errorMsg, EError eError) {
        super(errorMsg, eError);
    }
}
