package com.masrdelivery.exception;

/** Something prevents an order from being placed (checked). */
public abstract class OrderingException extends PlatformException {
    private static final long serialVersionUID = 1L;
    protected OrderingException(String message) { super(message); }
}
