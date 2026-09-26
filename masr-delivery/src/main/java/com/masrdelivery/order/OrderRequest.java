package com.masrdelivery.order;

import com.masrdelivery.exception.InvalidOrderException;
import com.masrdelivery.menu.LineItem;
import com.masrdelivery.menu.MenuItem;
import com.masrdelivery.model.Address;
import com.masrdelivery.model.Customer;
import com.masrdelivery.model.Restaurant;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;


public final class OrderRequest {

    private final Customer customer;
    private final Restaurant restaurant;
    private final Address deliveryAddress;
    private final List<LineItem> lines;
    private final Optional<String> promotionCode;
    private final Optional<String> notes;

    private OrderRequest(Builder b) {
        this.customer = b.customer;
        this.restaurant = b.restaurant;
        this.deliveryAddress = b.address;
        this.lines = List.copyOf(b.lines);
        this.promotionCode = Optional.ofNullable(b.promotionCode);
        this.notes = Optional.ofNullable(b.notes);
    }

    public static Builder builder() { return new Builder(); }

    public Customer customer()          { return customer; }
    public Restaurant restaurant()      { return restaurant; }
    public Address deliveryAddress()    { return deliveryAddress; }
    public List<LineItem> lines()       { return lines; }
    public Optional<String> promotionCode() { return promotionCode; }
    public Optional<String> notes()     { return notes; }

    public Map<MenuItem, BigDecimal> totalQuantities() { return totalQuantities(lines); }

    static Map<MenuItem, BigDecimal> totalQuantities(List<LineItem> lines) {
        return Collections.unmodifiableMap(lines.stream().collect(Collectors.toMap(
                LineItem::item, LineItem::quantity, BigDecimal::add, LinkedHashMap::new)));
    }

    public static final class Builder {
        private Customer customer;
        private Restaurant restaurant;
        private Address address;
        private final List<LineItem> lines = new ArrayList<>();
        private String promotionCode;
        private String notes;

        private Builder() {}

        public Builder customer(Customer c)     { this.customer = c; return this; }
        public Builder restaurant(Restaurant r) { this.restaurant = r; return this; }
        public Builder deliverTo(Address a)     { this.address = a; return this; }

        public Builder add(MenuItem item, BigDecimal quantity) { lines.add(new LineItem(item, quantity)); return this; }
        public Builder add(MenuItem item, int count)           { return add(item, BigDecimal.valueOf(count)); }
        public Builder add(MenuItem item, String quantity)     { return add(item, new BigDecimal(quantity)); }

        public Builder promotionCode(String code) {
            if (code == null || code.isBlank()) return this;
            if (promotionCode != null && !promotionCode.equalsIgnoreCase(code.trim()))
                throw new IllegalStateException("Only one promotion code may be used per order (already have "
                        + promotionCode + ").");
            this.promotionCode = code.trim();
            return this;
        }

        public Builder notes(String notes) {
            this.notes = notes == null || notes.isBlank() ? null : notes.trim();
            return this;
        }

        public int lineCount() { return lines.size(); }

        public OrderRequest build() throws InvalidOrderException {
            if (customer == null)   throw new InvalidOrderException("An order needs a customer.");
            if (restaurant == null) throw new InvalidOrderException("An order needs a restaurant.");
            if (address == null)    throw new InvalidOrderException("An order needs a delivery address.");
            if (lines.isEmpty())    throw new InvalidOrderException("An order must contain at least one item.");
            return new OrderRequest(this);
        }
    }
}
