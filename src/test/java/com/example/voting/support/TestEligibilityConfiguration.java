package com.example.voting.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
public class TestEligibilityConfiguration {

    @Bean
    @Primary
    ProgrammableEligibility testEligibility() {
        return new ProgrammableEligibility();
    }
}
