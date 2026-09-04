package com.example.voting.integration.userinfo;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The host is never hardcoded: the brief asks for the callback domain to be changeable by
 * configuration, so it comes from {@code voting.eligibility.base-url}. Both timeouts are always set,
 * because a call with no deadline can hold a request thread for as long as the upstream feels like.
 */
@ConfigurationProperties("voting.eligibility")
record EligibilityProperties(String baseUrl, Duration connectTimeout, Duration readTimeout) {}
