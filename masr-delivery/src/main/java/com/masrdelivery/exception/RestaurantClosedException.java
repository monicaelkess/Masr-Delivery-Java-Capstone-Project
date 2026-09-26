package com.masrdelivery.exception;

/** Order attempted against a restaurant that is currently closed. */
public class RestaurantClosedException extends OrderingException {
    private static final long serialVersionUID = 1L;
    public RestaurantClosedException(String message) { super(message); }
}
