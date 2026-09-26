package com.masrdelivery.promotion;

import com.masrdelivery.money.Money;

import java.util.List;


public final class FreeDeliveryPromotion extends Promotion {
    public FreeDeliveryPromotion(String code, List<PromotionCondition> conditions) { super(code, conditions); }
    @Override protected Money computeDiscount(PricingContext ctx) { return ctx.deliveryFee(); }
    @Override public String describeBenefit() { return "free delivery"; }
}
