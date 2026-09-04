package com.example.voting.vote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.voting.shared.errors.ConflictException;
import com.example.voting.support.IntegrationTest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The rule "one vote per member per topic" is enforced by a unique constraint rather than by reading
 * the table before inserting. This test is what makes that difference visible: a check-then-insert
 * would let several of these simultaneous requests through.
 */
@IntegrationTest
class ConcurrentVotingTest {

    private static final int SIMULTANEOUS_ATTEMPTS = 16;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VoteService votes;

    @Autowired
    private VoteRepository stored;

    @Test
    void acceptsOnlyOneVoteWhenTheSameMemberVotesManyTimesAtOnce() throws Exception {
        long topicId = topicWithAnOpenSession();
        CyclicBarrier allAtOnce = new CyclicBarrier(SIMULTANEOUS_ATTEMPTS);

        List<Future<Outcome>> attempts;
        try (ExecutorService pool = Executors.newFixedThreadPool(SIMULTANEOUS_ATTEMPTS)) {
            attempts = pool.invokeAll(IntStream.range(0, SIMULTANEOUS_ATTEMPTS)
                    .mapToObj(attempt -> castTogether(allAtOnce, topicId))
                    .toList());
        }

        assertThat(outcomes(attempts))
                .as("exactly one of %d simultaneous votes is accepted", SIMULTANEOUS_ATTEMPTS)
                .containsOnlyOnce(Outcome.ACCEPTED);
        assertThat(stored.countByTopicId(topicId)).isEqualTo(1);
    }

    private Callable<Outcome> castTogether(CyclicBarrier allAtOnce, long topicId) {
        return () -> {
            allAtOnce.await();
            try {
                votes.cast(topicId, "12345678901", Choice.YES);
                return Outcome.ACCEPTED;
            } catch (ConflictException rejected) {
                return Outcome.REJECTED;
            }
        };
    }

    private List<Outcome> outcomes(List<Future<Outcome>> attempts) throws Exception {
        List<Outcome> results = new ArrayList<>();
        for (Future<Outcome> attempt : attempts) {
            results.add(attempt.get());
        }
        return results;
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

    private enum Outcome {
        ACCEPTED,
        REJECTED
    }
}
