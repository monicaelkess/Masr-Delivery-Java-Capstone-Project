package com.masrdelivery.exception;

/** No record with that identifier. */
public class EntityNotFoundException extends EntityException {
    private static final long serialVersionUID = 1L;
    public EntityNotFoundException(String message) { super(message); }
}
