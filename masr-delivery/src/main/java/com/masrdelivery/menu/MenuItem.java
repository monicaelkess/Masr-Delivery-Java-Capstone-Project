package com.masrdelivery.menu;

import com.masrdelivery.money.Money;
import com.masrdelivery.model.Validation;

import java.math.BigDecimal;
import java.util.Objects;


public abstract class MenuItem {

    private final String restaurantId;
    private final String id;
    private final String name;
    private final String category;
    private final int prepMinutes;
    private volatile boolean available;

    protected MenuItem(String restaurantId, String id, String name, String category, int prepMinutes, boolean available) {
        this.restaurantId = Validation.requireText(restaurantId, "Restaurant id");
        this.id = Validation.requireText(id, "Item id");
        this.name = Validation.requireText(name, "Item name");
        this.category = Validation.requireText(category, "Category");
        this.prepMinutes = Validation.requirePositive(prepMinutes, "Preparation time");
        this.available = available;
    }

    /** Listed price for one unit (one piece, one bundle, or one kilogram). */
    public abstract Money unitPrice();

    /** Unit the customer orders in. */
    public abstract QuantityUnit unit();

    /** Price of a line of this item at the given quantity (already validated for the unit). */
    public abstract Money lineTotal(BigDecimal quantity);

    /** Type tag, as used in the data file. */
    public abstract String typeCode();

    /** Human-readable price, e.g. "85.00 EGP" or "420.00 EGP / kg". */
    public String priceLabel() { return unitPrice().toString(); }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public String restaurantId() { return restaurantId; }
    public String id()           { return id; }
    public String name()         { return name; }
    public String category()     { return category; }
    public int prepMinutes()     { return prepMinutes; }

    @Override public final boolean equals(Object o) {
        return o instanceof MenuItem m && restaurantId.equals(m.restaurantId) && id.equals(m.id);
    }
    @Override public final int hashCode() { return Objects.hash(restaurantId, id); }
    @Override public String toString() { return name + " (" + priceLabel() + ")"; }
}
