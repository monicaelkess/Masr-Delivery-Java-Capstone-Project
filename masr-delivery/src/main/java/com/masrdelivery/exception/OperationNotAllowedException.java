package com.masrdelivery.exception;

/** A request that is well-formed but not allowed in the current state (e.g. removing a restaurant with live orders). */
public class OperationNotAllowedException extends PlatformException {
    private static final long serialVersionUID = 1L;
    public OperationNotAllowedException(String message) { super(message); }
}
