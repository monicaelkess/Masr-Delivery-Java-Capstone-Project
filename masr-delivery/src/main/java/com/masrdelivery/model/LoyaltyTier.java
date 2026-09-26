package com.masrdelivery.model;

import com.masrdelivery.money.Money;

import java.util.Arrays;
import java.util.Comparator;

public enum LoyaltyTier {
    BRONZE(0, 0), SILVER(10, 10), GOLD(30, 100);

    private final int minCompletedOrders;
    private final int deliveryDiscountPercent;

    LoyaltyTier(int minCompletedOrders, int deliveryDiscountPercent) {
        this.minCompletedOrders = minCompletedOrders;
        this.deliveryDiscountPercent = deliveryDiscountPercent;
    }

    public static LoyaltyTier forCompletedOrders(int completed) {
        if (completed < 0) throw new IllegalArgumentException("negative order count");
        return Arrays.stream(values()).filter(t -> completed >= t.minCompletedOrders)
                .max(Comparator.comparingInt(t -> t.minCompletedOrders)).orElseThrow();
    }


    public Money applyToDeliveryFee(Money fee) {
        return fee.minus(fee.percent(deliveryDiscountPercent));
    }

    public int minCompletedOrders() { return minCompletedOrders; }
}
