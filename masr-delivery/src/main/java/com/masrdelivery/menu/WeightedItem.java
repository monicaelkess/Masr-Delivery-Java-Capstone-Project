package com.masrdelivery.menu;

import com.masrdelivery.money.Money;
import com.masrdelivery.model.Validation;

import java.math.BigDecimal;

public final class WeightedItem extends MenuItem {
    private final Money pricePerKg;

    public WeightedItem(String restaurantId, String id, String name, String category, int prepMinutes,
                        boolean available, Money pricePerKg) {
        super(restaurantId, id, name, category, prepMinutes, available);
        this.pricePerKg = Validation.requirePositive(pricePerKg, "Price per kg");
    }

    @Override public Money unitPrice() { return pricePerKg; }
    @Override public QuantityUnit unit() { return QuantityUnit.KILOGRAM; }
    @Override public Money lineTotal(BigDecimal kilograms) { return pricePerKg.times(unit().validate(kilograms)); }
    @Override public String typeCode() { return "WEIGHTED"; }
    @Override public String priceLabel() { return pricePerKg + " / kg"; }
}
