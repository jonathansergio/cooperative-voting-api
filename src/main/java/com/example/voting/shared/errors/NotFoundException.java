package com.example.voting.shared.errors;

/** Raised when a resource the caller referred to does not exist. Mapped to 404 by the handler. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
