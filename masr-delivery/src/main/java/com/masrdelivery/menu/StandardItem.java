package com.masrdelivery.menu;

import com.masrdelivery.money.Money;
import com.masrdelivery.model.Validation;

import java.math.BigDecimal;

/** Sold at its listed price per piece. */
public final class StandardItem extends MenuItem {
    private final Money price;

    public StandardItem(String restaurantId, String id, String name, String category, int prepMinutes,
                        boolean available, Money price) {
        super(restaurantId, id, name, category, prepMinutes, available);
        this.price = Validation.requirePositive(price, "Price");
    }

    @Override public Money unitPrice() { return price; }
    @Override public QuantityUnit unit() { return QuantityUnit.COUNT; }
    @Override public Money lineTotal(BigDecimal quantity) { return price.times(unit().validate(quantity)); }
    @Override public String typeCode() { return "STANDARD"; }
}
