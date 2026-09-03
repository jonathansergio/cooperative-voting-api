package com.example.voting.session;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.voting.support.IntegrationTest;
import com.example.voting.support.MutableClock;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class VotingSessionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MutableClock clock;

    @BeforeEach
    void startFromAKnownMoment() {
        clock.reset();
    }

    @Test
    void opensASessionForTheRequestedNumberOfMinutes() throws Exception {
        long topicId = registerTopic();

        mockMvc.perform(
                        post("/api/v1/topics/%d/sessions".formatted(topicId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"durationMinutes": 5}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.topicId").value(topicId))
                .andExpect(jsonPath("$.openedAt").value("2026-01-01T00:00:00Z"))
                .andExpect(jsonPath("$.closesAt").value("2026-01-01T00:05:00Z"))
                .andExpect(jsonPath("$.open").value(true));
    }

    @Test
    void opensASessionForOneMinuteWhenNoDurationIsGiven() throws Exception {
        long topicId = registerTopic();

        mockMvc.perform(post("/api/v1/topics/%d/sessions".formatted(topicId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.closesAt").value("2026-01-01T00:01:00Z"));
    }

    @Test
    void refusesToOpenASessionOnATopicThatDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/v1/topics/999999/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Topic 999999 not found"));
    }

    @Test
    void refusesASecondSessionOnTheSameTopic() throws Exception {
        long topicId = registerTopic();
        openSession(topicId);

        mockMvc.perform(post("/api/v1/topics/%d/sessions".formatted(topicId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Topic %d already has a voting session".formatted(topicId)));
    }

    @Test
    void reportsTheSessionAsClosedOnceItsTimeHasPassed() throws Exception {
        long topicId = registerTopic();
        long sessionId = openSession(topicId);

        clock.advanceBy(Duration.ofMinutes(1));

        mockMvc.perform(get("/api/v1/sessions/%d".formatted(sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.open").value(false));
    }

    @Test
    void reportsThatAnUnknownSessionDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/v1/sessions/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Voting session 999999 not found"));
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
        return idFrom(body);
    }

    private long openSession(long topicId) throws Exception {
        String body = mockMvc.perform(post("/api/v1/topics/%d/sessions".formatted(topicId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return idFrom(body);
    }

    private long idFrom(String json) {
        return Long.parseLong(json.replaceAll(".*\"id\"\\s*:\\s*(\\d+).*", "$1"));
    }
}
