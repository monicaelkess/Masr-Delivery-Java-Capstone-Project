package com.masrdelivery.event;

import com.masrdelivery.model.Customer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CustomerNotifier implements OrderStatusListener {
    private static final int INBOX_LIMIT = 50;
    private final Map<Customer, Deque<String>> inboxes = new ConcurrentHashMap<>();

    @Override public void onStatusChanged(OrderStatusChangedEvent e) {
        String msg = switch (e.to()) {
            case PLACED -> "Order " + e.order().id() + " placed with " + e.order().restaurant().name() + ".";
            case ACCEPTED -> e.order().restaurant().name() + " accepted order " + e.order().id() + ".";
            case PREPARING -> "Order " + e.order().id() + " is being prepared.";
            case READY -> "Order " + e.order().id() + " is ready and waiting for a rider.";
            case ASSIGNED -> "A rider has been assigned to order " + e.order().id() + ".";
            case OUT_FOR_DELIVERY -> "Order " + e.order().id() + " is on its way!";
            case DELIVERED -> "Order " + e.order().id() + " delivered. Enjoy your meal!";
            case CANCELLED -> "Order " + e.order().id() + " was cancelled"
                    + e.order().cancellationReason().map(r -> " (" + r + ")").orElse("") + ".";
        };
        Deque<String> inbox = inboxes.computeIfAbsent(e.order().customer(), c -> new ArrayDeque<>());
        synchronized (inbox) {
            inbox.addFirst("[" + e.at().toLocalTime().withNano(0) + "] " + msg);
            while (inbox.size() > INBOX_LIMIT) inbox.removeLast();
        }
    }


    public List<String> inbox(Customer c) {
        Deque<String> inbox = inboxes.get(c);
        if (inbox == null) return List.of();
        synchronized (inbox) { return List.copyOf(inbox); }
    }
}
