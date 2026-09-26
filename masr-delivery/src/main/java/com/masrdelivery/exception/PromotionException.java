package com.masrdelivery.exception;

/** A promotion code cannot be used on this order (checked). */
public abstract class PromotionException extends PlatformException {
    private static final long serialVersionUID = 1L;
    protected PromotionException(String message) { super(message); }
}
