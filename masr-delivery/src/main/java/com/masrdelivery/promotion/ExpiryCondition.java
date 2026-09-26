package com.masrdelivery.promotion;

import com.masrdelivery.exception.PromotionExpiredException;

import java.time.LocalDate;

/** Valid up to and including the given day. */
public record ExpiryCondition(LocalDate lastValidDay) implements PromotionCondition {
    @Override public void check(PricingContext ctx, String code) throws PromotionExpiredException {
        if (ctx.orderDate().isAfter(lastValidDay))
            throw new PromotionExpiredException("Promotion " + code + " expired on " + lastValidDay + ".");
    }
    @Override public String describe() { return "valid until " + lastValidDay; }
    @Override public int priority() { return 0; }
}
