package com.masrdelivery.service;

import com.masrdelivery.dispatch.DispatchService;
import com.masrdelivery.dispatch.Rider;
import com.masrdelivery.event.OrderEventBus;
import com.masrdelivery.exception.*;
import com.masrdelivery.menu.LineItem;
import com.masrdelivery.model.Customer;
import com.masrdelivery.model.LoyaltyTier;
import com.masrdelivery.model.Restaurant;
import com.masrdelivery.money.Money;
import com.masrdelivery.order.Order;
import com.masrdelivery.order.OrderRequest;
import com.masrdelivery.order.OrderStatus;
import com.masrdelivery.pricing.PriceBreakdown;
import com.masrdelivery.pricing.PricingEngine;
import com.masrdelivery.promotion.Promotion;
import com.masrdelivery.repo.InMemoryRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;


public final class OrderService {

    public record Tracking(Order order, OrderStatus status, Duration elapsed) {}

    private final InMemoryRepository<Order> orders;
    private final InMemoryRepository<Promotion> promotions;
    private final PricingEngine pricing;
    private final OrderEventBus events;
    private final DispatchService dispatch;
    private final InMemoryRepository<Rider> riders;
    private final Clock clock;
    private final AtomicLong sequence = new AtomicLong();

    public OrderService(InMemoryRepository<Order> orders, InMemoryRepository<Promotion> promotions, PricingEngine pricing,
                        OrderEventBus events, DispatchService dispatch, InMemoryRepository<Rider> riders, Clock clock) {
        this.orders = orders;
        this.promotions = promotions;
        this.pricing = pricing;
        this.events = events;
        this.dispatch = dispatch;
        this.riders = riders;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ customer

    /** Price without committing anything (used for a preview before confirming). */
    public PriceBreakdown quote(OrderRequest req) throws PlatformException {
        validate(req);
        return pricing.price(req.customer(), req.restaurant(), req.deliveryAddress(), req.lines(),
                resolvePromotion(req), LocalDate.now(clock), isFirstTimeCustomer(req.customer()));
    }

    /**
     * Validates, prices and places an order atomically: if anything fails, nothing
     * changes (no stock taken, no order stored, no event published).
     */
    public Order placeOrder(OrderRequest req) throws PlatformException {
        Customer customer = req.customer();
        synchronized (customer) {                 // serialises one customer's placements (first-order promo)
            PriceBreakdown price = quote(req);    // pure: validation + pricing, no side effects
            Restaurant restaurant = req.restaurant();
            restaurant.reserve(req.totalQuantities());   // atomic gate; re-checks open/available/stock under lock
            Order order = new Order(nextId(), req, price, LocalDateTime.now(clock), events);
            try {
                orders.add(order);
            } catch (DuplicateEntityException impossible) {
                restaurant.release(req.totalQuantities());
                throw impossible;
            }
            order.announcePlaced();
            return order;
        }
    }

    public Money pay(Customer customer, String orderId) throws PlatformException {
        Order order = ownedBy(customer, orderId);
        synchronized (order) {
            if (order.status() == OrderStatus.CANCELLED)
                throw new PaymentStateException("Order " + orderId + " is cancelled; nothing to pay.");
            if (order.isPaid()) throw new PaymentStateException("Order " + orderId + " is already paid.");
            customer.debit(order.price().total());       // throws InsufficientBalanceException, wallet untouched
            order.markPaid();
        }
        return customer.walletBalance();
    }

    /** @return the amount refunded to the wallet (zero if the order was never paid). */
    public Money cancelByCustomer(Customer customer, String orderId) throws PlatformException {
        return cancel(ownedBy(customer, orderId), "cancelled by customer");
    }

    public Tracking track(Customer customer, String orderId) throws PlatformException {
        Order o = ownedBy(customer, orderId);
        return new Tracking(o, o.status(), Duration.between(o.placedAt(), LocalDateTime.now(clock)));
    }

    public boolean isFirstTimeCustomer(Customer c) {
        return orders.all().stream().noneMatch(o -> o.customer().equals(c) && o.status() != OrderStatus.CANCELLED);
    }

    // ---------------------------------------------------------------- restaurant

    /** Restaurants only accept paid orders, so a cancellation can always be refunded. */
    public void accept(Restaurant r, String orderId) throws PlatformException {
        Order o = ownedBy(r, orderId);
        synchronized (o) {
            if (!o.isPaid() && o.status() == OrderStatus.PLACED)
                throw new PaymentStateException("Order " + orderId + " is not paid yet; it can be accepted once the customer pays.");
            o.transitionTo(OrderStatus.ACCEPTED, now());
        }
    }

    public Money reject(Restaurant r, String orderId) throws PlatformException {
        return cancel(ownedBy(r, orderId), "rejected by " + r.name());
    }

    public void startPreparing(Restaurant r, String orderId) throws PlatformException {
        ownedBy(r, orderId).transitionTo(OrderStatus.PREPARING, now());
    }

    public List<DispatchService.Assignment> markReady(Restaurant r, String orderId) throws PlatformException {
        Order o = ownedBy(r, orderId);
        synchronized (o) {
            o.transitionTo(OrderStatus.READY, now());
            dispatch.queue().enqueue(o, o.customer().tier() == LoyaltyTier.GOLD, now());
        }
        return dispatch.dispatchPending();
    }

    // --------------------------------------------------------------------- rider

    public List<DispatchService.Assignment> goOnDuty(Rider rider) {
        rider.goOnDuty();
        return dispatch.dispatchPending();
    }

    public void goOffDuty(Rider rider) throws RiderBusyException { rider.goOffDuty(); }

    public Optional<Order> currentOrder(Rider rider) { return rider.activeOrderId().flatMap(orders::find); }

    public Order pickUp(Rider rider) throws PlatformException {
        Order o = currentOrder(rider).orElseThrow(() -> new EntityNotFoundException("You have no assigned order."));
        o.transitionTo(OrderStatus.OUT_FOR_DELIVERY, now());
        return o;
    }

    public Order deliver(Rider rider) throws PlatformException {
        Order o = currentOrder(rider).orElseThrow(() -> new EntityNotFoundException("You have no assigned order."));
        o.transitionTo(OrderStatus.DELIVERED, now());         // rejects if not yet picked up
        rider.completeDelivery(o.id(), o.deliveryAddress().district());
        o.customer().recordCompletedOrder();                  // tier is derived from this count
        dispatch.dispatchPending();                           // rider is free again
        return o;
    }

    // ------------------------------------------------------------------- shared

    private Money cancel(Order order, String reason) throws IllegalOrderTransitionException {
        synchronized (order) {
            order.setCancellationReason(reason);
            try {
                order.transitionTo(OrderStatus.CANCELLED, now());   // the single legality check
            } catch (IllegalOrderTransitionException e) {
                order.setCancellationReason(null);
                throw e;
            }
            order.restaurant().release(order.totalQuantities());
            dispatch.queue().remove(order);
            order.riderId().flatMap(riders::find).ifPresent(r -> r.release(order.id()));
            if (order.isPaid()) {
                order.customer().credit(order.price().total());
                order.markRefunded();
                return order.price().total();
            }
            return Money.ZERO;
        }
    }

    private void validate(OrderRequest req) throws PlatformException {
        Restaurant r = req.restaurant();
        if (!r.isOpen()) throw new RestaurantClosedException(r.name() + " is closed right now.");
        if (!req.customer().ownsAddress(req.deliveryAddress()))
            throw new InvalidOrderException("The delivery address must be one of your saved addresses.");
        for (LineItem line : req.lines()) {
            if (!line.item().restaurantId().equals(r.id()) || r.findItem(line.item().id()).isEmpty())
                throw new ItemUnavailableException(line.item().name() + " is not on " + r.name() + "'s menu.");
            if (!line.item().isAvailable())
                throw new ItemUnavailableException(line.item().name() + " is currently unavailable.");
        }
        for (var e : req.totalQuantities().entrySet()) {
            var left = r.stockOf(e.getKey().id());
            if (left.compareTo(e.getValue()) < 0)
                throw new StockShortageException("Only " + e.getKey().unit().format(left) + " of " + e.getKey().name()
                        + " left today; you asked for " + e.getKey().unit().format(e.getValue()) + ".");
        }
    }

    private Optional<Promotion> resolvePromotion(OrderRequest req) throws UnknownPromotionException {
        if (req.promotionCode().isEmpty()) return Optional.empty();
        String code = Promotion.normalize(req.promotionCode().get());
        return Optional.of(promotions.find(code)
                .orElseThrow(() -> new UnknownPromotionException("No promotion with code " + code + ".")));
    }

    private Order ownedBy(Customer c, String orderId) throws EntityNotFoundException, OwnershipException {
        Order o = orders.get(orderId);
        if (!o.customer().equals(c)) throw new OwnershipException("Order " + orderId + " does not belong to you.");
        return o;
    }

    private Order ownedBy(Restaurant r, String orderId) throws EntityNotFoundException, OwnershipException {
        Order o = orders.get(orderId);
        if (!o.restaurant().equals(r)) throw new OwnershipException("Order " + orderId + " is not for " + r.name() + ".");
        return o;
    }

    private String nextId() { return String.format("ORD-%05d", sequence.incrementAndGet()); }
    private LocalDateTime now() { return LocalDateTime.now(clock); }
}
