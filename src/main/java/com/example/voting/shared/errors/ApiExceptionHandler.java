package com.example.voting.shared.errors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Translates every failure into an RFC 7807 response. Domain code raises meaning, never HTTP, and the
 * mapping lives here alone.
 *
 * <p>Extending {@link ResponseEntityExceptionHandler} keeps Spring's own answers for malformed
 * requests (400, 404, 405 and friends); the handlers below add the domain's refusals and a last
 * resort for anything nobody anticipated.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail handleNotFound(NotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail handleConflict(ConflictException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(UnprocessableException.class)
    ProblemDetail handleUnprocessable(UnprocessableException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, exception.getMessage());
    }

    @ExceptionHandler(UpstreamUnavailableException.class)
    ProblemDetail handleUpstreamUnavailable(UpstreamUnavailableException exception) {
        // The cause carries the upstream's own message; it stays in the log and never in the response.
        log.warn("A system this request depends on did not answer", exception);
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
    }

    /**
     * An unforeseen failure is a defect. Its message can name tables, SQL or hosts, so it goes to the
     * log alongside the correlation id and the caller gets only enough to report it.
     */
    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception) {
        log.error("Unexpected failure while serving the request", exception);
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Quote the X-Correlation-Id response header when reporting it.");
    }
}
