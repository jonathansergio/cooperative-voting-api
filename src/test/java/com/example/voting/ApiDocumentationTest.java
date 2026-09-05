package com.example.voting;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.voting.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The published contract is part of the deliverable, so it is checked like any other behaviour: if an
 * endpoint stops being described, this fails.
 */
@IntegrationTest
class ApiDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publishesAnOpenApiDocumentNamingTheApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value("Cooperative Voting API"));
    }

    @Test
    void describesEveryEndpointOfTheApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/topics'].post.summary").exists())
                .andExpect(
                        jsonPath("$.paths['/api/v1/topics/{id}'].get.summary").exists())
                .andExpect(jsonPath("$.paths['/api/v1/topics/{topicId}/sessions'].post.summary")
                        .exists())
                .andExpect(
                        jsonPath("$.paths['/api/v1/sessions/{id}'].get.summary").exists())
                .andExpect(jsonPath("$.paths['/api/v1/topics/{topicId}/votes'].post.summary")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/topics/{topicId}/result'].get.summary")
                        .exists());
    }

    @Test
    void documentsTheFailuresACallerHasToHandle() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/topics/{topicId}/votes'].post.responses.409")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/topics/{topicId}/votes'].post.responses.422")
                        .exists());
    }

    @Test
    void servesTheInteractiveDocumentation() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
