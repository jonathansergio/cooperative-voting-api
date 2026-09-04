package com.example.voting.vote;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.voting.support.IntegrationTest;
import com.example.voting.support.MutableClock;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@IntegrationTest
class VoteResultTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MutableClock clock;

    @Test
    void countsTheVotesOnEachSide() throws Exception {
        long topicId = topicWithAnOpenSession();
        vote(topicId, "11111111111", "YES");
        vote(topicId, "22222222222", "YES");
        vote(topicId, "33333333333", "NO");

        result(topicId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topicId").value(topicId))
                .andExpect(jsonPath("$.yes").value(2))
                .andExpect(jsonPath("$.no").value(1))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.outcome").value("APPROVED"))
                .andExpect(jsonPath("$.votingOpen").value(true));
    }

    @Test
    void reportsRejectionWhenMostVotesAreAgainst() throws Exception {
        long topicId = topicWithAnOpenSession();
        vote(topicId, "11111111111", "NO");
        vote(topicId, "22222222222", "YES");
        vote(topicId, "33333333333", "NO");

        result(topicId).andExpect(jsonPath("$.outcome").value("REJECTED"));
    }

    @Test
    void reportsATieWhenBothSidesMatch() throws Exception {
        long topicId = topicWithAnOpenSession();
        vote(topicId, "11111111111", "YES");
        vote(topicId, "22222222222", "NO");

        result(topicId).andExpect(jsonPath("$.outcome").value("TIED"));
    }

    @Test
    void reportsAnEmptyResultWhenNobodyHasVoted() throws Exception {
        long topicId = topicWithAnOpenSession();

        result(topicId)
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.outcome").value("TIED"));
    }

    @Test
    void stopsReportingTheVotingAsOpenOnceTheSessionCloses() throws Exception {
        long topicId = topicWithAnOpenSession();
        vote(topicId, "11111111111", "YES");
        clock.advanceBy(Duration.ofMinutes(1));

        result(topicId)
                .andExpect(jsonPath("$.votingOpen").value(false))
                .andExpect(jsonPath("$.outcome").value("APPROVED"));
    }

    @Test
    void refusesAResultForATopicThatWasNeverPutToVote() throws Exception {
        long topicId = registerTopic();

        result(topicId)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Topic %d has no voting session".formatted(topicId)));
    }

    @Test
    void refusesAResultForATopicThatDoesNotExist() throws Exception {
        result(999999)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Topic 999999 not found"));
    }

    private ResultActions result(long topicId) throws Exception {
        return mockMvc.perform(get("/api/v1/topics/%d/result".formatted(topicId)));
    }

    private void vote(long topicId, String memberId, String choice) throws Exception {
        mockMvc.perform(post("/api/v1/topics/%d/votes".formatted(topicId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\": \"%s\", \"choice\": \"%s\"}".formatted(memberId, choice)))
                .andExpect(status().isCreated());
    }

    private long topicWithAnOpenSession() throws Exception {
        long topicId = registerTopic();
        mockMvc.perform(post("/api/v1/topics/%d/sessions".formatted(topicId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated());
        return topicId;
    }

    private long registerTopic() throws Exception {
        String body = mockMvc.perform(
                        post("/api/v1/topics")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"title": "Prestação de contas"}
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return Long.parseLong(body.replaceAll(".*\"id\"\\s*:\\s*(\\d+).*", "$1"));
    }
}
