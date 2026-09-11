package com.example.voting.vote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.voting.eligibility.EligibilityStatus;
import com.example.voting.support.IntegrationTest;
import com.example.voting.support.ProgrammableEligibility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@IntegrationTest
class VoteEligibilityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProgrammableEligibility eligibility;

    @Test
    void checksEligibilityWithoutHoldingADatabaseTransactionOpen() throws Exception {
        long topicId = topicWithAnOpenSession();

        vote(topicId, "12345678901").andExpect(status().isCreated());

        assertThat(eligibility.lastCheckRanInsideTransaction())
                .as("the eligibility lookup is a network call and must not hold a pooled connection")
                .isFalse();
    }

    @Test
    void refusesTheVoteOfAMemberWhoIsNotAllowedToVote() throws Exception {
        long topicId = topicWithAnOpenSession();
        eligibility.answer("12345678901", EligibilityStatus.UNABLE_TO_VOTE);

        vote(topicId, "12345678901")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Member 12345678901 is not allowed to vote"));
    }

    @Test
    void refusesTheVoteOfSomeoneTheRegistryDoesNotKnow() throws Exception {
        long topicId = topicWithAnOpenSession();
        eligibility.answer("00000000000", EligibilityStatus.UNKNOWN_MEMBER);

        vote(topicId, "00000000000")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Member 00000000000 is not a known member"));
    }

    @Test
    void acceptsTheVoteOfAnEligibleMember() throws Exception {
        long topicId = topicWithAnOpenSession();
        eligibility.answer("12345678901", EligibilityStatus.ABLE_TO_VOTE);

        vote(topicId, "12345678901").andExpect(status().isCreated());
    }

    private ResultActions vote(long topicId, String memberId) throws Exception {
        return mockMvc.perform(post("/api/v1/topics/%d/votes".formatted(topicId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\": \"%s\", \"choice\": \"YES\"}".formatted(memberId)));
    }

    private long topicWithAnOpenSession() throws Exception {
        String body = mockMvc.perform(
                        post("/api/v1/topics")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"title": "Reforma do estatuto"}
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long topicId = Long.parseLong(body.replaceAll(".*\"id\"\\s*:\\s*(\\d+).*", "$1"));

        mockMvc.perform(post("/api/v1/topics/%d/sessions".formatted(topicId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated());
        return topicId;
    }
}
