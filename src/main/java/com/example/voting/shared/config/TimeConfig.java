package com.example.voting.shared.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The application reads the current time from an injected clock rather than from {@code
 * Instant.now()}, so time-dependent rules can be exercised without waiting for real time to pass.
 */
@Configuration(proxyBeanMethods = false)
class TimeConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
