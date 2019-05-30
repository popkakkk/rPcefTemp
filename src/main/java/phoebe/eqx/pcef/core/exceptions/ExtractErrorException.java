package phoebe.eqx.pcef.core.exceptions;

import phoebe.eqx.pcef.enums.EError;

public class ExtractErrorException extends PCEFException {

    public ExtractErrorException(String errorMsg, EError eError) {
        super(errorMsg, eError);
    }
}
