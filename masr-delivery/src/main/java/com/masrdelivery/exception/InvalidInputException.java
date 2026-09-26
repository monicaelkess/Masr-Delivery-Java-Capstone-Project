package com.masrdelivery.exception;

/**
 * UNCHECKED. A precondition the caller could have verified itself was violated:
 * a non-positive price, a rating outside 0-5, a malformed mobile number, a blank name.
 * The console validates raw input before calling the domain, so reaching one of
 * these from the UI indicates a bug; it is still reported, never swallowed.
 */
public class InvalidInputException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;
    public InvalidInputException(String message) { super(message); }
}
