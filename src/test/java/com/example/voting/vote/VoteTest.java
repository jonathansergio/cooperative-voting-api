package com.example.voting.vote;

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
class VoteTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MutableClock clock;

    @Test
    void recordsAVoteOnAnOpenSession() throws Exception {
        long topicId = topicWithAnOpenSession();

        vote(topicId, "12345678901", "YES")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.topicId").value(topicId))
                .andExpect(jsonPath("$.memberId").value("12345678901"))
                .andExpect(jsonPath("$.choice").value("YES"))
                .andExpect(jsonPath("$.castAt").value("2026-01-01T00:00:00Z"));
    }

    @Test
    void refusesASecondVoteFromTheSameMember() throws Exception {
        long topicId = topicWithAnOpenSession();
        vote(topicId, "12345678901", "YES").andExpect(status().isCreated());

        vote(topicId, "12345678901", "NO")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail")
                        .value("Member 12345678901 has already voted on topic %d".formatted(topicId)));
    }

    @Test
    void letsDifferentMembersVoteOnTheSameTopic() throws Exception {
        long topicId = topicWithAnOpenSession();

        vote(topicId, "11111111111", "YES").andExpect(status().isCreated());
        vote(topicId, "22222222222", "NO").andExpect(status().isCreated());
    }

    @Test
    void refusesAVoteAfterTheSessionHasClosed() throws Exception {
        long topicId = topicWithAnOpenSession();
        clock.advanceBy(Duration.ofMinutes(1));

        vote(topicId, "12345678901", "YES")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("The voting session for topic %d is closed".formatted(topicId)));
    }

    @Test
    void refusesAVoteWhenTheTopicHasNoSession() throws Exception {
        long topicId = registerTopic();

        vote(topicId, "12345678901", "YES")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Topic %d has no voting session".formatted(topicId)));
    }

    @Test
    void refusesAVoteOnATopicThatDoesNotExist() throws Exception {
        vote(999999, "12345678901", "YES")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Topic 999999 not found"));
    }

    @Test
    void refusesAChoiceThatIsNeitherYesNorNo() throws Exception {
        long topicId = topicWithAnOpenSession();

        vote(topicId, "12345678901", "MAYBE").andExpect(status().isBadRequest());
    }

    private ResultActions vote(long topicId, String memberId, String choice) throws Exception {
        return mockMvc.perform(post("/api/v1/topics/%d/votes".formatted(topicId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\": \"%s\", \"choice\": \"%s\"}".formatted(memberId, choice)));
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
                                {"title": "Reforma do estatuto"}
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return Long.parseLong(body.replaceAll(".*\"id\"\\s*:\\s*(\\d+).*", "$1"));
    }
}
