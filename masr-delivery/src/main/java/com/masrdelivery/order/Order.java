package com.masrdelivery.order;

import com.masrdelivery.event.OrderEventBus;
import com.masrdelivery.event.OrderStatusChangedEvent;
import com.masrdelivery.exception.IllegalOrderTransitionException;
import com.masrdelivery.exception.PaymentStateException;
import com.masrdelivery.menu.LineItem;
import com.masrdelivery.menu.MenuItem;
import com.masrdelivery.model.Address;
import com.masrdelivery.model.Customer;
import com.masrdelivery.model.Identifiable;
import com.masrdelivery.model.Restaurant;
import com.masrdelivery.pricing.PriceBreakdown;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;


public final class Order implements Identifiable {

    private final String id;
    private final Customer customer;
    private final Restaurant restaurant;
    private final Address deliveryAddress;
    private final List<LineItem> lines;
    private final Optional<String> notes;
    private final PriceBreakdown price;
    private final LocalDateTime placedAt;
    private final OrderEventBus events;

    // guarded by this
    private OrderStatus status = OrderStatus.PLACED;
    private final Map<OrderStatus, LocalDateTime> timeline = new EnumMap<>(OrderStatus.class);
    private String riderId;
    private boolean paid;
    private boolean refunded;
    private String cancellationReason;

    public Order(String id, OrderRequest request, PriceBreakdown price, LocalDateTime placedAt, OrderEventBus events) {
        this.id = id;
        this.customer = request.customer();
        this.restaurant = request.restaurant();
        this.deliveryAddress = request.deliveryAddress();
        this.lines = request.lines();
        this.notes = request.notes();
        this.price = price;
        this.placedAt = placedAt;
        this.events = events;
        this.timeline.put(OrderStatus.PLACED, placedAt);
    }

    public void announcePlaced() { events.publish(new OrderStatusChangedEvent(this, null, OrderStatus.PLACED, placedAt)); }

    public void transitionTo(OrderStatus next, LocalDateTime at) throws IllegalOrderTransitionException {
        OrderStatus from;
        synchronized (this) {
            from = status;
            if (!from.canTransitionTo(next)) throw new IllegalOrderTransitionException(id, from, next);
            status = next;
            timeline.put(next, at);
        }
        events.publish(new OrderStatusChangedEvent(this, from, next, at));
    }

    public void assignRider(String riderId, LocalDateTime at) throws IllegalOrderTransitionException {
        synchronized (this) {
            if (!status.canTransitionTo(OrderStatus.ASSIGNED) || status != OrderStatus.READY)
                throw new IllegalOrderTransitionException(id, status, OrderStatus.ASSIGNED);
            this.riderId = riderId;
            status = OrderStatus.ASSIGNED;
            timeline.put(OrderStatus.ASSIGNED, at);
        }
        events.publish(new OrderStatusChangedEvent(this, OrderStatus.READY, OrderStatus.ASSIGNED, at));
    }

    public synchronized void markPaid() throws PaymentStateException {
        if (status == OrderStatus.CANCELLED) throw new PaymentStateException("Order " + id + " is cancelled; nothing to pay.");
        if (paid) throw new PaymentStateException("Order " + id + " is already paid.");
        paid = true;
    }

    public synchronized void markRefunded()               { refunded = true; }
    public synchronized void setCancellationReason(String r) { cancellationReason = r; }

    @Override public String id()          { return id; }
    public Customer customer()            { return customer; }
    public Restaurant restaurant()        { return restaurant; }
    public Address deliveryAddress()      { return deliveryAddress; }
    public List<LineItem> lines()         { return lines; }
    public Optional<String> notes()       { return notes; }
    public PriceBreakdown price()         { return price; }
    public LocalDateTime placedAt()       { return placedAt; }

    public synchronized OrderStatus status()           { return status; }
    public synchronized Optional<String> riderId()     { return Optional.ofNullable(riderId); }
    public synchronized boolean isPaid()               { return paid; }
    public synchronized boolean isRefunded()           { return refunded; }
    public synchronized Optional<String> cancellationReason() { return Optional.ofNullable(cancellationReason); }
    public synchronized Optional<LocalDateTime> timeOf(OrderStatus s) { return Optional.ofNullable(timeline.get(s)); }
    public synchronized Map<OrderStatus, LocalDateTime> timeline() { return Collections.unmodifiableMap(new EnumMap<>(timeline)); }


    public Optional<Duration> deliveryDuration() {
        return timeOf(OrderStatus.OUT_FOR_DELIVERY)
                .flatMap(out -> timeOf(OrderStatus.DELIVERED).map(done -> Duration.between(out, done)));
    }

    public Map<MenuItem, BigDecimal> totalQuantities() { return OrderRequest.totalQuantities(lines); }
    public int parcelCount() { return lines.stream().mapToInt(LineItem::parcels).sum(); }

    @Override public boolean equals(Object o) { return o instanceof Order other && id.equals(other.id); }
    @Override public int hashCode() { return id.hashCode(); }
    @Override public String toString() { return id + " (" + status() + ", " + price.total() + ")"; }
}
