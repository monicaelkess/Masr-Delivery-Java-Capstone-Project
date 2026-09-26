package com.masrdelivery.exception;

/** Lookup / registration failures (checked). */
public abstract class EntityException extends PlatformException {
    private static final long serialVersionUID = 1L;
    protected EntityException(String message) { super(message); }
}
