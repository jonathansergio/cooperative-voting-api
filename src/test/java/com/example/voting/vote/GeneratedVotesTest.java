package com.example.voting.vote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.voting.support.IntegrationTest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The hand-written vote tests pick a few scenarios. These generate many — random members, random
 * repeats, random order — and check the rules that must hold for all of them. The seed is fixed, so a
 * failure reproduces exactly, and the scenario that broke is printed in the assertion message.
 */
@IntegrationTest
class GeneratedVotesTest {

    private static final long SEED = 20260911L;
    private static final int SCENARIOS = 15;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void acceptsExactlyTheFirstVoteOfEachMemberWhateverTheSequence() throws Exception {
        Random random = new Random(SEED);
        for (int scenario = 0; scenario < SCENARIOS; scenario++) {
            List<Ballot> ballots = randomBallots(random);
            long topicId = topicWithAnOpenSession();
            Map<String, Choice> firstChoices = new LinkedHashMap<>();

            for (Ballot ballot : ballots) {
                int answered = cast(topicId, ballot);
                boolean isFirst = firstChoices.putIfAbsent(ballot.memberId(), ballot.choice()) == null;
                assertThat(answered)
                        .as("scenario %d %s: %s", scenario, ballots, isFirst ? "first vote" : "repeated vote")
                        .isEqualTo(isFirst ? 201 : 409);
            }
        }
    }

    @Test
    void countsExactlyTheVotesItAcceptedWhateverTheSequence() throws Exception {
        Random random = new Random(SEED + 1);
        for (int scenario = 0; scenario < SCENARIOS; scenario++) {
            List<Ballot> ballots = randomBallots(random);
            long topicId = topicWithAnOpenSession();
            Map<String, Choice> firstChoices = new LinkedHashMap<>();
            for (Ballot ballot : ballots) {
                cast(topicId, ballot);
                firstChoices.putIfAbsent(ballot.memberId(), ballot.choice());
            }

            long expectedYes =
                    firstChoices.values().stream().filter(Choice.YES::equals).count();
            long expectedNo = firstChoices.size() - expectedYes;
            String result = mockMvc.perform(get("/api/v1/topics/%d/result".formatted(topicId)))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            assertThat(result)
                    .as("scenario %d %s", scenario, ballots)
                    .contains("\"yes\":%d".formatted(expectedYes))
                    .contains("\"no\":%d".formatted(expectedNo))
                    .contains("\"total\":%d".formatted(firstChoices.size()));
        }
    }

    private List<Ballot> randomBallots(Random random) {
        int members = 1 + random.nextInt(10);
        int ballots = 1 + random.nextInt(30);
        List<Ballot> sequence = new ArrayList<>();
        IntStream.range(0, ballots)
                .forEach(i -> sequence.add(new Ballot(
                        "%011d".formatted(random.nextInt(members)), random.nextBoolean() ? Choice.YES : Choice.NO)));
        return sequence;
    }

    private int cast(long topicId, Ballot ballot) throws Exception {
        return mockMvc.perform(post("/api/v1/topics/%d/votes".formatted(topicId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\": \"%s\", \"choice\": \"%s\"}"
                                .formatted(ballot.memberId(), ballot.choice())))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    private long topicWithAnOpenSession() throws Exception {
        String body = mockMvc.perform(post("/api/v1/topics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Pauta gerada\"}"))
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

    private record Ballot(String memberId, Choice choice) {
        @Override
        public String toString() {
            return memberId.substring(memberId.length() - 1) + ":" + choice;
        }
    }
}
