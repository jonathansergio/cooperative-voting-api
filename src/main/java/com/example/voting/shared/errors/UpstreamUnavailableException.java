package com.example.voting.shared.errors;

/**
 * Raised when a system this application depends on could not answer. Mapped to 503: the request was
 * not refused on its merits, it simply could not be decided right now.
 */
public class UpstreamUnavailableException extends RuntimeException {

    public UpstreamUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
