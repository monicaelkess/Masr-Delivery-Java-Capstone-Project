package com.masrdelivery.event;

import com.masrdelivery.order.Order;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class RiderDashboard implements OrderStatusListener {
    private final Map<String, String> cards = new ConcurrentHashMap<>();

    @Override public void onStatusChanged(OrderStatusChangedEvent e) {
        Order o = e.order();
        o.riderId().ifPresent(rider -> {
            switch (e.to()) {
                case ASSIGNED, OUT_FOR_DELIVERY -> cards.put(rider, card(o, e.to().name()));
                case DELIVERED, CANCELLED -> cards.remove(rider);
                default -> { }
            }
        });
    }

    private static String card(Order o, String stage) {
        return o.id() + " [" + stage + "] pick up at " + o.restaurant().name() + " (" + o.restaurant().district()
                + ") -> " + o.deliveryAddress() + ", " + o.parcelCount() + " parcel(s)"
                + o.notes().map(n -> ", note: " + n).orElse("");
    }

    public Optional<String> cardFor(String riderId) { return Optional.ofNullable(cards.get(riderId)); }
    public Map<String, String> snapshot() { return Map.copyOf(cards); }
}
