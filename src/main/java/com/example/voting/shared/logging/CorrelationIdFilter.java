package com.example.voting.shared.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gives every request an id that appears on all of its log lines and comes back to the caller. When
 * an assembly has many members voting at once, this is what lets a single member's request be read
 * as one story instead of scattered lines. A caller that already has an id sends it and keeps it, so
 * a trace survives across systems.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class CorrelationIdFilter extends OncePerRequestFilter {

    static final String HEADER = "X-Correlation-Id";
    static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String correlationId = correlationIdOf(request);
        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER, correlationId);
        try {
            chain.doFilter(request, response);
        } finally {
            // The thread goes back to the pool and will serve someone else's request next.
            MDC.remove(MDC_KEY);
        }
    }

    private String correlationIdOf(HttpServletRequest request) {
        String supplied = request.getHeader(HEADER);
        return StringUtils.hasText(supplied) ? supplied : UUID.randomUUID().toString();
    }
}
