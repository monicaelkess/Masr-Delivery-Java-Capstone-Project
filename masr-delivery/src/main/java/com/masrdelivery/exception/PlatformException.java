package com.masrdelivery.exception;

/**
 * Root of every Masr Delivery business failure.
 *
 * RULE (applied to every type in this package):
 *   CHECKED   - the failure comes from the state of the world at the moment of the
 *               call (a restaurant closed, stock ran out, another thread took the rider).
 *               A perfectly written caller cannot rule it out in advance, so the
 *               compiler forces it to decide what to tell the user.
 *   UNCHECKED - the caller broke a precondition it could have checked itself
 *               (negative price, malformed mobile number, null argument). That is a
 *               bug or bad raw input at the boundary: see {@link InvalidInputException}.
 *
 * Catching PlatformException catches every business failure and nothing else from
 * the JVM; catching a mid-level category (OrderingException, PaymentException, ...)
 * catches just that family.
 */
public abstract class PlatformException extends Exception {
    private static final long serialVersionUID = 1L;
    protected PlatformException(String message) { super(message); }
}
