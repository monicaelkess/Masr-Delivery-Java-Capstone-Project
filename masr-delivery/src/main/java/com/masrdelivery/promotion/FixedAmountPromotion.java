package com.masrdelivery.promotion;

import com.masrdelivery.money.Money;
import com.masrdelivery.model.Validation;

import java.util.List;

/** A fixed amount off the subtotal; limited to the subtotal so it never eats into fees. */
public final class FixedAmountPromotion extends Promotion {
    private final Money amount;

    public FixedAmountPromotion(String code, Money amount, List<PromotionCondition> conditions) {
        super(code, conditions);
        this.amount = Validation.requirePositive(amount, "Discount amount");
    }

    @Override protected Money computeDiscount(PricingContext ctx) { return amount.min(ctx.subtotal()); }
    @Override public String describeBenefit() { return amount + " off the subtotal"; }
}
