package com.masrdelivery.event;

import com.masrdelivery.money.Money;
import com.masrdelivery.order.OrderStatus;

import java.util.EnumMap;
import java.util.Map;

public final class PlatformStatistics implements OrderStatusListener {
    private final Map<OrderStatus, Long> transitionsInto = new EnumMap<>(OrderStatus.class);
    private Money deliveredRevenue = Money.ZERO;
    private Money cancelledValue = Money.ZERO;

    @Override public synchronized void onStatusChanged(OrderStatusChangedEvent e) {
        transitionsInto.merge(e.to(), 1L, Long::sum);
        if (e.to() == OrderStatus.DELIVERED) deliveredRevenue = deliveredRevenue.plus(e.order().price().total());
        if (e.to() == OrderStatus.CANCELLED) cancelledValue = cancelledValue.plus(e.order().price().total());
    }

    public synchronized long ordersPlaced()     { return transitionsInto.getOrDefault(OrderStatus.PLACED, 0L); }
    public synchronized long ordersDelivered()  { return transitionsInto.getOrDefault(OrderStatus.DELIVERED, 0L); }
    public synchronized long ordersCancelled()  { return transitionsInto.getOrDefault(OrderStatus.CANCELLED, 0L); }
    public synchronized Money deliveredRevenue(){ return deliveredRevenue; }
    public synchronized Money cancelledValue()  { return cancelledValue; }
    public synchronized Map<OrderStatus, Long> transitions() { return Map.copyOf(transitionsInto); }
}
