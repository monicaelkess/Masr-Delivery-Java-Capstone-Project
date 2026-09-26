package com.masrdelivery.exception;

/** Menu item is switched off or does not belong to the restaurant. */
public class ItemUnavailableException extends OrderingException {
    private static final long serialVersionUID = 1L;
    public ItemUnavailableException(String message) { super(message); }
}
