package com.masrdelivery.promotion;

import com.masrdelivery.exception.PromotionNotApplicableException;
import com.masrdelivery.money.Money;

public record MinimumSubtotalCondition(Money minimum) implements PromotionCondition {
    @Override public void check(PricingContext ctx, String code) throws PromotionNotApplicableException {
        if (ctx.subtotal().isLessThan(minimum))
            throw new PromotionNotApplicableException("Promotion " + code + " needs a subtotal of at least " + minimum
                    + "; your subtotal is " + ctx.subtotal() + " (add " + minimum.minus(ctx.subtotal()) + " more).");
    }
    @Override public String describe() { return "minimum subtotal " + minimum; }
}
