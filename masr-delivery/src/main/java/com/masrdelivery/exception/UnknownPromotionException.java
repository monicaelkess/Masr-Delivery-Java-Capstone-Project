package com.masrdelivery.exception;

/** No promotion exists with that code. */
public class UnknownPromotionException extends PromotionException {
    private static final long serialVersionUID = 1L;
    public UnknownPromotionException(String message) { super(message); }
}
