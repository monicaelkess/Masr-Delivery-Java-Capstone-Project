package com.masrdelivery.event;

/** OBSERVER: anything that reacts to an order changing status. */
@FunctionalInterface
public interface OrderStatusListener {
    void onStatusChanged(OrderStatusChangedEvent event);
}
