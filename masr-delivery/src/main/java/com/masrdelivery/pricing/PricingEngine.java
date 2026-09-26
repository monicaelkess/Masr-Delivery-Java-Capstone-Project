package com.masrdelivery.pricing;

import com.masrdelivery.config.PlatformConfig;
import com.masrdelivery.exception.PromotionException;
import com.masrdelivery.menu.LineItem;
import com.masrdelivery.model.Address;
import com.masrdelivery.model.Customer;
import com.masrdelivery.model.LoyaltyTier;
import com.masrdelivery.model.Restaurant;
import com.masrdelivery.money.Money;
import com.masrdelivery.money.MoneyCollectors;
import com.masrdelivery.promotion.PricingContext;
import com.masrdelivery.promotion.Promotion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public final class PricingEngine {

    private final PlatformConfig config;

    public PricingEngine(PlatformConfig config) { this.config = config; }

    public PriceBreakdown price(Customer customer, Restaurant restaurant, Address address, List<LineItem> lines,
                                Optional<Promotion> promotion, LocalDate orderDate, boolean firstTimeCustomer)
            throws PromotionException {
        // 1. Subtotal
        Money subtotal = lines.stream().collect(MoneyCollectors.summing(LineItem::lineTotal));

        // 2. Delivery fee: base + per-km for every started km beyond the free distance, then loyalty
        BigDecimal km = config.distances().between(restaurant.district(), address.district());
        Money baseFee = baseDeliveryFee(km);
        LoyaltyTier tier = customer.tier();
        Money deliveryFee = tier.applyToDeliveryFee(baseFee);

        // 3. Service fee on the subtotal (before promotion), rounded to the piastre
        Money serviceFee = subtotal.percent(config.serviceFeePercent());

        // 4. Promotion (at most one; Optional makes "one" structural)
        PricingContext ctx = new PricingContext(customer, restaurant, address.district(), subtotal, deliveryFee,
                orderDate, firstTimeCustomer);
        Money discount = promotion.isPresent() ? promotion.get().discountFor(ctx) : Money.ZERO;

        // 5. Total, never negative
        Money total = subtotal.plus(deliveryFee).plus(serviceFee).minus(discount).max(Money.ZERO);

        return new PriceBreakdown(subtotal, km, baseFee, tier, deliveryFee, serviceFee,
                promotion.map(Promotion::code), discount, total);
    }

    public Money baseDeliveryFee(BigDecimal km) {
        BigDecimal chargeableKm = km.subtract(config.freeKm()).max(BigDecimal.ZERO).setScale(0, RoundingMode.CEILING);
        return config.baseDeliveryFee().plus(config.perKmFee().times(chargeableKm));
    }
}
