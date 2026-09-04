package com.example.voting.integration.userinfo;

import com.example.voting.eligibility.EligibilityChecker;
import com.example.voting.eligibility.EligibilityStatus;
import com.example.voting.shared.errors.UpstreamUnavailableException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Talks to the member registry described in the brief: {@code GET /users/{cpf}} answers either that
 * the member can vote or that they cannot, and 404 means the registry does not know that CPF.
 */
class UserInfoEligibilityChecker implements EligibilityChecker {

    private final RestClient client;

    UserInfoEligibilityChecker(RestClient client) {
        this.client = client;
    }

    @Override
    public EligibilityStatus statusOf(String memberId) {
        try {
            UserInfoResponse answer = client.get()
                    .uri("/users/{cpf}", memberId)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, response) -> {})
                    .body(UserInfoResponse.class);
            return answer == null ? EligibilityStatus.UNKNOWN_MEMBER : answer.toStatus();
        } catch (RestClientException unreachable) {
            // Never guess. An unanswered eligibility check is not a "no", and it is not a "yes".
            throw new UpstreamUnavailableException("The eligibility registry did not answer", unreachable);
        }
    }
}
