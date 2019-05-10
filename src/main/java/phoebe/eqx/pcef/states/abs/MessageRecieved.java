package phoebe.eqx.pcef.states.abs;

;

import phoebe.eqx.pcef.enums.state.EState;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Only usable on methods.
@Target(ElementType.METHOD)
// Annotation must be available during runtime.
@Retention(RetentionPolicy.RUNTIME)
public @interface MessageRecieved {
    EState messageType();
}
