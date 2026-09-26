package com.masrdelivery.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;


public final class Money implements Comparable<Money>, java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    public static final Money ZERO = new Money(BigDecimal.ZERO);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final BigDecimal amount;

    private Money(BigDecimal amount) {
        this.amount = Objects.requireNonNull(amount, "amount").setScale(SCALE, ROUNDING);
    }

    public static Money of(BigDecimal amount) { return new Money(amount); }
    public static Money of(String amount)     { return new Money(new BigDecimal(amount.trim())); }
    public static Money of(long pounds)       { return new Money(BigDecimal.valueOf(pounds)); }

    public Money plus(Money other)  { return new Money(amount.add(other.amount)); }
    public Money minus(Money other) { return new Money(amount.subtract(other.amount)); }

    /** Multiply by an exact factor (a count, kilograms, a ratio); result rounded to the piastre. */
    public Money times(BigDecimal factor) { return new Money(amount.multiply(factor)); }
    public Money times(long factor)       { return times(BigDecimal.valueOf(factor)); }

    public Money percent(BigDecimal pct) { return new Money(amount.multiply(pct).divide(HUNDRED, 10, ROUNDING)); }
    public Money percent(long pct)       { return percent(BigDecimal.valueOf(pct)); }

    public Money dividedBy(long divisor) {
        if (divisor == 0) throw new ArithmeticException("divide by zero");
        return new Money(amount.divide(BigDecimal.valueOf(divisor), SCALE, ROUNDING));
    }

    public Money min(Money other) { return compareTo(other) <= 0 ? this : other; }
    public Money max(Money other) { return compareTo(other) >= 0 ? this : other; }

    public boolean isNegative() { return amount.signum() < 0; }
    public boolean isPositive() { return amount.signum() > 0; }
    public boolean isZero()     { return amount.signum() == 0; }
    public boolean isLessThan(Money other) { return compareTo(other) < 0; }

    public BigDecimal amount() { return amount; }

    @Override public int compareTo(Money o) { return amount.compareTo(o.amount); }

    // Scale is always 2, so BigDecimal.equals (which is scale-sensitive) is safe here.
    @Override public boolean equals(Object o) { return o instanceof Money m && amount.equals(m.amount); }
    @Override public int hashCode() { return amount.hashCode(); }
    @Override public String toString() { return amount.toPlainString() + " EGP"; }
}
