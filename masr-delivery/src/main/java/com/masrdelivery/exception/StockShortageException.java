package com.masrdelivery.exception;

/** Requested quantity exceeds today's remaining stock. */
public class StockShortageException extends OrderingException {
    private static final long serialVersionUID = 1L;
    public StockShortageException(String message) { super(message); }
}
