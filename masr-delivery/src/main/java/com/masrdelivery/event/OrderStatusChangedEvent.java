package com.masrdelivery.event;

import com.masrdelivery.order.Order;
import com.masrdelivery.order.OrderStatus;

import java.time.LocalDateTime;
import java.util.Optional;


public record OrderStatusChangedEvent(Order order, OrderStatus fromOrNull, OrderStatus to, LocalDateTime at) {
    public Optional<OrderStatus> from() { return Optional.ofNullable(fromOrNull); }
}
