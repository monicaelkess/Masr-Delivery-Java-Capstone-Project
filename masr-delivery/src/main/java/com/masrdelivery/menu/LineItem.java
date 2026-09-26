package com.masrdelivery.menu;

import com.masrdelivery.money.Money;
import com.masrdelivery.model.Validation;

import java.math.BigDecimal;

public record LineItem(MenuItem item, BigDecimal quantity) {
    public LineItem {
        Validation.requireNonNull(item, "Menu item");
        quantity = item.unit().validate(quantity);
    }

    public Money lineTotal() { return item.lineTotal(quantity); }

        public int parcels() { return item.unit() == QuantityUnit.COUNT ? quantity.intValueExact() : 1; }

    @Override public String toString() {
        return (item.unit() == QuantityUnit.COUNT ? quantity.toPlainString() + " x " : quantity.toPlainString() + " kg ")
                + item.name() + " = " + lineTotal();
    }
}
