package com.example.voting.shared.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Records how each request ended and how long it took. Only the method, the path, the status and the
 * duration: never the body, which for this API carries the identifier of the member who voted.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long startedAt = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long millis = (System.nanoTime() - startedAt) / 1_000_000;
            log.info("{} {} -> {} in {}ms", request.getMethod(), request.getRequestURI(), response.getStatus(), millis);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Health probes are polled constantly and would drown out the requests that matter.
        return request.getRequestURI().startsWith("/actuator/health");
    }
}
