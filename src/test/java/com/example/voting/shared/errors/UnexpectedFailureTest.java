package com.example.voting.shared.errors;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.voting.support.IntegrationTest;
import com.example.voting.support.ProgrammableEligibility;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * A defect nobody anticipated still has to reach the caller as a clean 500, and the message it
 * carries internally — table names, SQL, hostnames — must stay in the log.
 */
@IntegrationTest
class UnexpectedFailureTest {

    private static final String INTERNAL_DETAIL = "relation \"secret_table\" does not exist";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProgrammableEligibility eligibility;

    @Test
    void answersAnUnforeseenFailureWithAGenericProblemAndNoInternalDetail() throws Exception {
        long topicId = topicWithAnOpenSession();
        eligibility.failWith(new IllegalStateException(INTERNAL_DETAIL));

        mockMvc.perform(post("/api/v1/topics/%d/votes".formatted(topicId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\": \"12345678901\", \"choice\": \"YES\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(content().string(not(Matchers.containsString("secret_table"))));
    }

    private long topicWithAnOpenSession() throws Exception {
        String body = mockMvc.perform(post("/api/v1/topics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Reforma do estatuto\"}"))
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
