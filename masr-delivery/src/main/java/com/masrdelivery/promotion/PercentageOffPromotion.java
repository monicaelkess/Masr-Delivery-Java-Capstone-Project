package com.masrdelivery.promotion;

import com.masrdelivery.exception.InvalidInputException;
import com.masrdelivery.money.Money;
import com.masrdelivery.model.Validation;

import java.math.BigDecimal;
import java.util.List;

public final class PercentageOffPromotion extends Promotion {
    private final BigDecimal percent;
    private final Money cap;

    public PercentageOffPromotion(String code, BigDecimal percent, Money cap, List<PromotionCondition> conditions) {
        super(code, conditions);
        if (percent == null || percent.signum() <= 0 || percent.compareTo(BigDecimal.valueOf(100)) > 0)
            throw new InvalidInputException("Percentage must be in (0, 100].");
        this.percent = percent;
        this.cap = Validation.requirePositive(cap, "Discount cap");
    }

    @Override protected Money computeDiscount(PricingContext ctx) {
        return ctx.subtotal().percent(percent).min(cap).min(ctx.subtotal());
    }

    @Override public String describeBenefit() {
        return percent.stripTrailingZeros().toPlainString() + "% off the subtotal, max " + cap;
    }
}
