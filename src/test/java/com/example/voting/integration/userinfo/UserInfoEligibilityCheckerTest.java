package com.example.voting.integration.userinfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.example.voting.eligibility.EligibilityStatus;
import com.example.voting.shared.errors.UpstreamUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * The upstream in the brief answers at random and may well be offline, so its contract is pinned
 * here against a mocked transport instead of against the real service.
 */
class UserInfoEligibilityCheckerTest {

    private static final String BASE_URL = "https://user-info.example.com";
    private static final String CPF = "12345678901";

    private MockRestServiceServer upstream;
    private UserInfoEligibilityChecker checker;

    private void givenUpstreamAnswers(org.springframework.test.web.client.ResponseCreator answer) {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        upstream = MockRestServiceServer.bindTo(builder).build();
        upstream.expect(requestTo(BASE_URL + "/users/" + CPF))
                .andExpect(method(HttpMethod.GET))
                .andRespond(answer);
        checker = new UserInfoEligibilityChecker(builder.build());
    }

    @Test
    void readsThatTheMemberIsAllowedToVote() {
        givenUpstreamAnswers(withSuccess(
                """
                {"status": "ABLE_TO_VOTE"}
                """, MediaType.APPLICATION_JSON));

        assertThat(checker.statusOf(CPF)).isEqualTo(EligibilityStatus.ABLE_TO_VOTE);
        upstream.verify();
    }

    @Test
    void readsThatTheMemberIsNotAllowedToVote() {
        givenUpstreamAnswers(withSuccess(
                """
                {"status": "UNABLE_TO_VOTE"}
                """, MediaType.APPLICATION_JSON));

        assertThat(checker.statusOf(CPF)).isEqualTo(EligibilityStatus.UNABLE_TO_VOTE);
    }

    @Test
    void treatsAnUnknownCpfAsAMemberThatDoesNotExist() {
        givenUpstreamAnswers(withResourceNotFound());

        assertThat(checker.statusOf(CPF)).isEqualTo(EligibilityStatus.UNKNOWN_MEMBER);
    }

    @Test
    void refusesToGuessWhenTheUpstreamFails() {
        givenUpstreamAnswers(withServerError());

        assertThatThrownBy(() -> checker.statusOf(CPF))
                .isInstanceOf(UpstreamUnavailableException.class)
                .hasMessageContaining("eligibility");
    }
}
