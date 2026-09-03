package com.example.voting.shared.errors;

/**
 * Raised when the request is well formed but the current state of the domain does not allow it.
 * Mapped to 422.
 */
public class UnprocessableException extends RuntimeException {

    public UnprocessableException(String message) {
        super(message);
    }
}
