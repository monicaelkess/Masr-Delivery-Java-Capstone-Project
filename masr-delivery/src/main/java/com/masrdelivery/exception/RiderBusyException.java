package com.masrdelivery.exception;

/** Rider already holds an active order: the "one active order" invariant. */
public class RiderBusyException extends DispatchException {
    private static final long serialVersionUID = 1L;
    public RiderBusyException(String riderId, String activeOrderId) {
        super("Rider " + riderId + " is already carrying order " + activeOrderId + ".");
    }
}
