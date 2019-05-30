package phoebe.eqx.pcef.core.exceptions;

import phoebe.eqx.pcef.enums.EError;

public class MissingParameterException extends PCEFException {


    public MissingParameterException(String errorMsg, EError eError) {
        super(errorMsg, eError);
    }
}
