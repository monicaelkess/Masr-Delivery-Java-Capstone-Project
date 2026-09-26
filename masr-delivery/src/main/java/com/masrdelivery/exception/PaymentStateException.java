package com.masrdelivery.exception;

/** Order already paid, or cancelled, so it cannot be paid. */
public class PaymentStateException extends PaymentException {
    private static final long serialVersionUID = 1L;
    public PaymentStateException(String message) { super(message); }
}
