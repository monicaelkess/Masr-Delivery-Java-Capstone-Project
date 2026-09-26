package com.masrdelivery.dispatch;

import com.masrdelivery.order.Order;

import java.time.LocalDateTime;
import java.util.*;

public final class DispatchQueue {

    private record Entry(Order order, boolean gold, LocalDateTime readyAt, long sequence) {}

    private static final Comparator<Entry> PRIORITY = Comparator
            .comparing((Entry e) -> !e.gold())
            .thenComparing(Entry::readyAt)
            .thenComparingLong(Entry::sequence);

    private final PriorityQueue<Entry> queue = new PriorityQueue<>(PRIORITY);
    private final Map<Order, Entry> index = new HashMap<>();
    private long nextSequence;

    public synchronized boolean enqueue(Order order, boolean goldCustomer, LocalDateTime readyAt) {
        if (index.containsKey(order)) return false;
        Entry e = new Entry(order, goldCustomer, readyAt, nextSequence++);
        index.put(order, e);
        return queue.add(e);
    }

    public synchronized boolean remove(Order order) {
        Entry e = index.remove(order);
        return e != null && queue.remove(e);
    }

    public synchronized Optional<Order> peek() { return Optional.ofNullable(queue.peek()).map(Entry::order); }

       public synchronized List<Order> inDispatchOrder() {
        return queue.stream().sorted(PRIORITY).map(Entry::order).toList();
    }

    public synchronized int size() { return queue.size(); }
    public synchronized boolean contains(Order o) { return index.containsKey(o); }
}
