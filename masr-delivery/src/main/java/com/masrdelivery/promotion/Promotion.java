package com.masrdelivery.promotion;

import com.masrdelivery.exception.InvalidInputException;
import com.masrdelivery.exception.PromotionException;
import com.masrdelivery.money.Money;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;


public abstract class Promotion {

    private final String code;
    private final List<PromotionCondition> conditions;

    protected Promotion(String code, List<PromotionCondition> conditions) {
        this.code = normalize(code);
        if (!this.code.matches("[A-Z0-9_-]{3,20}"))
            throw new InvalidInputException("Promotion code must be 3-20 letters, digits, '-' or '_': " + code);
        this.conditions = conditions == null ? List.of()
                : conditions.stream().sorted(Comparator.comparingInt(PromotionCondition::priority)).toList();
    }

    public static String normalize(String code) {
        if (code == null || code.isBlank()) throw new InvalidInputException("Promotion code must not be blank.");
        return code.trim().toUpperCase(Locale.ROOT);
    }

    public final Money discountFor(PricingContext ctx) throws PromotionException {
        for (PromotionCondition c : conditions) c.check(ctx, code);
        return computeDiscount(ctx).max(Money.ZERO);
    }

    /** Conditions are already satisfied when this runs. */
    protected abstract Money computeDiscount(PricingContext ctx);

    public abstract String describeBenefit();

    public String code() { return code; }
    public List<PromotionCondition> conditions() { return conditions; }

    public String describe() {
        return code + ": " + describeBenefit() + (conditions.isEmpty() ? "" :
                " [" + conditions.stream().map(PromotionCondition::describe).collect(Collectors.joining("; ")) + "]");
    }

    @Override public final boolean equals(Object o) { return o instanceof Promotion p && code.equals(p.code); }
    @Override public final int hashCode() { return code.hashCode(); }
    @Override public String toString() { return describe(); }
}
