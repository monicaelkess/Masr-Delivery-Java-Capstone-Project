package com.masrdelivery.exception;

/** Order lifecycle failures (checked). */
public abstract class OrderStateException extends PlatformException {
    private static final long serialVersionUID = 1L;
    protected OrderStateException(String message) { super(message); }
}
