package phoebe.eqx.pcef.core.exceptions;

import phoebe.eqx.pcef.enums.EError;

public class WrongFormatException extends PCEFException {


    public WrongFormatException(String errorMsg, EError eError) {
        super(errorMsg, eError);
    }
}
