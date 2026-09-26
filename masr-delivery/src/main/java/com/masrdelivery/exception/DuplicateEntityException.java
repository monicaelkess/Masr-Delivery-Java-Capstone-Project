package com.masrdelivery.exception;

/** A record with that identifier already exists. */
public class DuplicateEntityException extends EntityException {
    private static final long serialVersionUID = 1L;
    public DuplicateEntityException(String message) { super(message); }
}
