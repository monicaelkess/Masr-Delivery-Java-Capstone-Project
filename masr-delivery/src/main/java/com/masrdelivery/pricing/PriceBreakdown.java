package com.masrdelivery.pricing;

import com.masrdelivery.model.LoyaltyTier;
import com.masrdelivery.money.Money;

import java.math.BigDecimal;
import java.util.Optional;

public record PriceBreakdown(Money subtotal, BigDecimal distanceKm, Money baseDeliveryFee, LoyaltyTier tier,
                             Money deliveryFee, Money serviceFee, Optional<String> promotionCode,
                             Money promotionDiscount, Money total) {

    public String format() {
        StringBuilder sb = new StringBuilder();
        sb.append(row("Subtotal", subtotal.toString()));
        sb.append(row("Delivery (" + distanceKm.stripTrailingZeros().toPlainString() + " km)", baseDeliveryFee.toString()));
        if (!baseDeliveryFee.equals(deliveryFee))
            sb.append(row("  " + tier + " loyalty benefit", deliveryFee.minus(baseDeliveryFee).toString()));
        sb.append(row("Service fee", serviceFee.toString()));
        promotionCode.ifPresent(c -> sb.append(row("Promotion " + c, Money.ZERO.minus(promotionDiscount).toString())));
        sb.append(row("TOTAL", total.toString()));
        return sb.toString();
    }

    private static String row(String label, String value) { return String.format("  %-30s %16s%n", label, value); }
}
