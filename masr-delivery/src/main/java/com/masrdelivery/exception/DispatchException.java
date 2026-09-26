package com.masrdelivery.exception;

/** Rider assignment failures (checked). */
public abstract class DispatchException extends PlatformException {
    private static final long serialVersionUID = 1L;
    protected DispatchException(String message) { super(message); }
}
