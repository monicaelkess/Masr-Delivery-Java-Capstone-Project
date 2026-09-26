package com.masrdelivery.exception;

/** Caller acts on something that is not theirs (another customer's order, another restaurant's order). */
public class OwnershipException extends PlatformException {
    private static final long serialVersionUID = 1L;
    public OwnershipException(String message) { super(message); }
}
