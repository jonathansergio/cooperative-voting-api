package com.example.voting.shared.errors;

/** Raised when the request clashes with the current state of a resource. Mapped to 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
