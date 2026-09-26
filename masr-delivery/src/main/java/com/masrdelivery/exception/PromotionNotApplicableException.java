package com.masrdelivery.exception;

/** A promotion condition (minimum, district, first-order) is not met. */
public class PromotionNotApplicableException extends PromotionException {
    private static final long serialVersionUID = 1L;
    public PromotionNotApplicableException(String message) { super(message); }
}
