package com.masrdelivery.exception;

import com.masrdelivery.order.OrderStatus;

/** Rejected lifecycle move, e.g. PLACED -> DELIVERED, or cancelling a DELIVERED order. */
public class IllegalOrderTransitionException extends OrderStateException {
    private static final long serialVersionUID = 1L;
    private final OrderStatus from;
    private final OrderStatus to;

    public IllegalOrderTransitionException(String orderId, OrderStatus from, OrderStatus to) {
        super("Order " + orderId + " cannot move from " + from + " to " + to
                + (from.isTerminal() ? " (" + from + " is final)." : ". Allowed next: " + from.allowedNext()));
        this.from = from;
        this.to = to;
    }
    public OrderStatus from() { return from; }
    public OrderStatus to()   { return to; }
}
