package com.example.voting.topic;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.voting.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class TopicRegistrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registersATopicAndReturnsWhereToFindIt() throws Exception {
        mockMvc.perform(
                        post("/api/v1/topics")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"title": "Reforma do estatuto", "description": "Votação da nova redação"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Reforma do estatuto"))
                .andExpect(jsonPath("$.description").value("Votação da nova redação"));
    }

    @Test
    void rejectsATopicWithoutATitle() throws Exception {
        mockMvc.perform(
                        post("/api/v1/topics")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"title": "   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }
}
