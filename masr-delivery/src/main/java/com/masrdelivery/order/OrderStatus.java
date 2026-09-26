package com.masrdelivery.order;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;


public enum OrderStatus {
    PLACED, ACCEPTED, PREPARING, READY, ASSIGNED, OUT_FOR_DELIVERY, DELIVERED, CANCELLED;

    private static final Map<OrderStatus, Set<OrderStatus>> NEXT = new EnumMap<>(OrderStatus.class);
    static {
        NEXT.put(PLACED,           EnumSet.of(ACCEPTED, CANCELLED));
        NEXT.put(ACCEPTED,         EnumSet.of(PREPARING, CANCELLED));
        NEXT.put(PREPARING,        EnumSet.of(READY, CANCELLED));
        NEXT.put(READY,            EnumSet.of(ASSIGNED, CANCELLED));
        NEXT.put(ASSIGNED,         EnumSet.of(OUT_FOR_DELIVERY, CANCELLED));
        NEXT.put(OUT_FOR_DELIVERY, EnumSet.of(DELIVERED));
        NEXT.put(DELIVERED,        EnumSet.noneOf(OrderStatus.class));
        NEXT.put(CANCELLED,        EnumSet.noneOf(OrderStatus.class));
    }

    public boolean canTransitionTo(OrderStatus next) { return NEXT.get(this).contains(next); }
    public Set<OrderStatus> allowedNext() { return Collections.unmodifiableSet(NEXT.get(this)); }
    public boolean isTerminal() { return NEXT.get(this).isEmpty(); }
}
