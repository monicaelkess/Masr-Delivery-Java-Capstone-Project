package com.masrdelivery.exception;

/** Wallet / payment failures (checked). */
public abstract class PaymentException extends PlatformException {
    private static final long serialVersionUID = 1L;
    protected PaymentException(String message) { super(message); }
}
