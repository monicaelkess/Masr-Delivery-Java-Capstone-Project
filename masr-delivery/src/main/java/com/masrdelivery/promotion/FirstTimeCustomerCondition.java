package com.masrdelivery.promotion;

import com.masrdelivery.exception.PromotionNotApplicableException;

/** Only for a customer with no previous non-cancelled order. */
public record FirstTimeCustomerCondition() implements PromotionCondition {
    @Override public void check(PricingContext ctx, String code) throws PromotionNotApplicableException {
        if (!ctx.firstTimeCustomer())
            throw new PromotionNotApplicableException("Promotion " + code + " is for first-time customers only.");
    }
    @Override public String describe() { return "first order only"; }
}
