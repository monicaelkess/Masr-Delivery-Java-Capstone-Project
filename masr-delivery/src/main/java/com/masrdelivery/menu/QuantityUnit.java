package com.masrdelivery.menu;

import com.masrdelivery.exception.InvalidInputException;

import java.math.BigDecimal;

public enum QuantityUnit {
    COUNT("x") {
        @Override public BigDecimal validate(BigDecimal q) {
            requirePositive(q);
            if (q.stripTrailingZeros().scale() > 0)
                throw new InvalidInputException("This item is ordered by count; quantity must be a whole number: " + q.toPlainString());
            return q;
        }
    },
    KILOGRAM("kg") {
        @Override public BigDecimal validate(BigDecimal q) {
            requirePositive(q);
            if (q.stripTrailingZeros().scale() > 3)
                throw new InvalidInputException("Weight is accepted to the gram (max 3 decimals): " + q.toPlainString());
            return q;
        }
    };

    private final String symbol;
    QuantityUnit(String symbol) { this.symbol = symbol; }
    public String symbol() { return symbol; }

    public abstract BigDecimal validate(BigDecimal quantity);

    /** "5" for counts, "1.5 kg" for weights. */
    public String format(BigDecimal quantity) {
        String n = quantity.stripTrailingZeros().toPlainString();
        return this == KILOGRAM ? n + " kg" : n;
    }

    private static void requirePositive(BigDecimal q) {
        if (q == null || q.signum() <= 0) throw new InvalidInputException("Quantity must be greater than zero.");
    }
}
