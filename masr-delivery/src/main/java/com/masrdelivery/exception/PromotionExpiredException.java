package com.masrdelivery.exception;

/** Promotion past its expiry date. */
public class PromotionExpiredException extends PromotionException {
    private static final long serialVersionUID = 1L;
    public PromotionExpiredException(String message) { super(message); }
}
