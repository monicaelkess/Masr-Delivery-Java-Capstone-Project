package com.masrdelivery.exception;

/** Rider is off duty. */
public class RiderUnavailableException extends DispatchException {
    private static final long serialVersionUID = 1L;
    public RiderUnavailableException(String message) { super(message); }
}
