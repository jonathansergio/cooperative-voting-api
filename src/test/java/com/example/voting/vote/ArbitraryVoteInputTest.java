package com.example.voting.vote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.voting.support.IntegrationTest;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Whatever a client sends to the vote endpoint — garbage, missing fields, wrong types, oversized
 * values, other content types — the answer may be a refusal, but never a server error. A 500 here
 * means an input nobody thought about reached code that did not expect it.
 */
@IntegrationTest
class ArbitraryVoteInputTest {

    private static final long SEED = 20260912L;
    private static final int ATTEMPTS = 200;

    private static final List<String> MEMBER_IDS = List.of(
            "\"12345678901\"",
            "\"\"",
            "\"   \"",
            "null",
            "123",
            "true",
            "[]",
            "{}",
            "\"" + "9".repeat(64) + "\"",
            "\"" + "9".repeat(65) + "\"",
            "\"associado-ção-✓\"",
            "\"\\u0000\"",
            "\"'; drop table votes; --\"");

    private static final List<String> CHOICES =
            List.of("\"YES\"", "\"NO\"", "\"yes\"", "\"TALVEZ\"", "\"\"", "null", "0", "false", "[\"YES\"]", "{}");

    private static final List<Function<Random, String>> BODIES = List.of(
            random -> "{\"memberId\": %s, \"choice\": %s}".formatted(pick(random, MEMBER_IDS), pick(random, CHOICES)),
            random -> "{\"memberId\": %s}".formatted(pick(random, MEMBER_IDS)),
            random -> "{\"choice\": %s}".formatted(pick(random, CHOICES)),
            random -> "{}",
            random -> "",
            random -> "not json at all",
            random -> "[{\"memberId\": \"1\", \"choice\": \"YES\"}]",
            random -> "{\"memberId\": \"1\", \"choice\": \"YES\"",
            random -> "{\"memberId\": \"1\", \"choice\": \"YES\", \"extra\": %s}".formatted(pick(random, CHOICES)));

    private static final List<String> TOPIC_SEGMENTS = List.of("%d", "999999999", "0", "-1", "abc", "9".repeat(30));

    private static final List<MediaType> CONTENT_TYPES =
            List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.APPLICATION_XML);

    @Autowired
    private MockMvc mockMvc;

    @Test
    void neverAnswersWithAServerErrorWhateverTheRequestContains() throws Exception {
        long openTopicId = topicWithAnOpenSession();
        Random random = new Random(SEED);

        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            String topic = pick(random, TOPIC_SEGMENTS).formatted(openTopicId);
            String body = pick(random, BODIES).apply(random);
            MockHttpServletRequestBuilder request =
                    post("/api/v1/topics/" + topic + "/votes").content(body);
            if (random.nextInt(5) > 0) {
                request.contentType(pick(random, CONTENT_TYPES));
            }

            int answered = mockMvc.perform(request).andReturn().getResponse().getStatus();

            assertThat(answered)
                    .as("attempt %d: POST /api/v1/topics/%s/votes with body [%s]", attempt, topic, body)
                    .isLessThan(500);
        }
    }

    private static <T> T pick(Random random, List<T> options) {
        return options.get(random.nextInt(options.size()));
    }

    private long topicWithAnOpenSession() throws Exception {
        String body = mockMvc.perform(post("/api/v1/topics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Pauta para entradas arbitrárias\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long topicId = Long.parseLong(body.replaceAll(".*\"id\"\\s*:\\s*(\\d+).*", "$1"));
        mockMvc.perform(post("/api/v1/topics/%d/sessions".formatted(topicId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"durationMinutes\": 30}"))
                .andExpect(status().isCreated());
        return topicId;
    }
}
