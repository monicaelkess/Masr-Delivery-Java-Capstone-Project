package com.masrdelivery.exception;

/** Structurally invalid order: no lines, missing customer, foreign address... */
public class InvalidOrderException extends OrderingException {
    private static final long serialVersionUID = 1L;
    public InvalidOrderException(String message) { super(message); }
}
