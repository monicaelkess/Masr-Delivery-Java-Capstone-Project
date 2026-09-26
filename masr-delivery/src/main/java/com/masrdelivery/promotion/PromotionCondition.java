package com.masrdelivery.promotion;

import com.masrdelivery.exception.PromotionException;

/** A restriction on when a promotion applies. New restrictions are new classes. */
public interface PromotionCondition {
    /** @throws PromotionException with a customer-facing explanation when not satisfied. */
    void check(PricingContext ctx, String promoCode) throws PromotionException;

    String describe();

    /** Lower runs first, so "expired" is reported before "subtotal too low". */
    default int priority() { return 10; }
}
