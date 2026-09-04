package com.example.voting.integration.userinfo;

import com.example.voting.eligibility.EligibilityChecker;
import com.example.voting.eligibility.EligibilityStatus;
import java.net.http.HttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * The registry the brief points at is a free-tier host that is very likely offline, and the exercise
 * still has to be runnable. So the lookup has two modes, chosen by {@code voting.eligibility.mode}:
 * {@code remote} is the default and talks to the real service, while {@code stub} answers that
 * everyone may vote and exists for local runs and for CI.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(EligibilityProperties.class)
class EligibilityConfig {

    @Bean
    @ConditionalOnProperty(name = "voting.eligibility.mode", havingValue = "remote", matchIfMissing = true)
    EligibilityChecker remoteEligibilityChecker(EligibilityProperties properties) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build());
        requestFactory.setReadTimeout(properties.readTimeout());
        return new UserInfoEligibilityChecker(RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build());
    }

    @Bean
    @ConditionalOnProperty(name = "voting.eligibility.mode", havingValue = "stub")
    EligibilityChecker stubEligibilityChecker() {
        return memberId -> EligibilityStatus.ABLE_TO_VOTE;
    }
}
