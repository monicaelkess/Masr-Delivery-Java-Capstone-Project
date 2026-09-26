package com.masrdelivery.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


public final class OrderEventBus {
    private final List<OrderStatusListener> listeners = new CopyOnWriteArrayList<>();
    private final List<String> listenerFailures = new CopyOnWriteArrayList<>();

    public void subscribe(OrderStatusListener l)   { listeners.add(l); }
    public void unsubscribe(OrderStatusListener l) { listeners.remove(l); }

    public void publish(OrderStatusChangedEvent event) {
        for (OrderStatusListener l : listeners) {
            try {
                l.onStatusChanged(event);
            } catch (RuntimeException e) {
                listenerFailures.add(l.getClass().getSimpleName() + " failed on " + event.order().id() + ": " + e);
            }
        }
    }

    public List<String> listenerFailures() { return List.copyOf(listenerFailures); }
}
