package com.example.voting.screens;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.voting.support.IntegrationTest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * The screen contract from the brief's annex: the mobile client renders whatever the server sends,
 * so the shape of these messages is the deliverable and is pinned here field by field.
 */
@IntegrationTest
class ScreenFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsTopicsOpenForVotingAsASelectionScreen() throws Exception {
        long topicId = topicWithAnOpenSession("Reforma do estatuto");

        mockMvc.perform(get("/api/v1/screens/topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("SELECAO"))
                .andExpect(jsonPath("$.titulo").exists())
                .andExpect(
                        jsonPath("$.itens[?(@.texto == 'Reforma do estatuto')]").exists())
                .andExpect(jsonPath("$.itens[?(@.texto == 'Reforma do estatuto')].url")
                        .value(Matchers.hasItem(Matchers.startsWith("http://localhost:8080"))))
                .andExpect(jsonPath("$.itens[?(@.texto == 'Reforma do estatuto')].url")
                        .value(Matchers.hasItem(
                                Matchers.endsWith("/api/v1/screens/topics/%d/identify".formatted(topicId)))));
    }

    @Test
    void leavesOutTopicsThatAreNotAcceptingVotes() throws Exception {
        registerTopic("Pauta sem sessão");

        mockMvc.perform(get("/api/v1/screens/topics"))
                .andExpect(jsonPath("$.itens[?(@.texto == 'Pauta sem sessão')]").doesNotExist());
    }

    @Test
    void asksWhoIsVotingOnAFormScreen() throws Exception {
        long topicId = topicWithAnOpenSession("Prestação de contas");

        mockMvc.perform(post("/api/v1/screens/topics/%d/identify".formatted(topicId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("FORMULARIO"))
                .andExpect(jsonPath("$.titulo").value("Prestação de contas"))
                .andExpect(jsonPath("$.itens[?(@.tipo == 'INPUT_TEXTO')].id").value(Matchers.hasItem("memberId")))
                .andExpect(jsonPath("$.botaoOk.url")
                        .value(Matchers.endsWith("/api/v1/screens/topics/%d/choose".formatted(topicId))))
                .andExpect(jsonPath("$.botaoCancelar.url").exists());
    }

    @Test
    void offersYesAndNoOnASelectionScreenCarryingWhoIsVoting() throws Exception {
        long topicId = topicWithAnOpenSession("Reforma do estatuto");

        mockMvc.perform(
                        post("/api/v1/screens/topics/%d/choose".formatted(topicId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"memberId": "12345678901"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("SELECAO"))
                .andExpect(jsonPath("$.itens[0].texto").value("Sim"))
                .andExpect(jsonPath("$.itens[0].body.choice").value("YES"))
                .andExpect(jsonPath("$.itens[0].body.memberId").value("12345678901"))
                .andExpect(jsonPath("$.itens[1].texto").value("Não"))
                .andExpect(jsonPath("$.itens[1].body.choice").value("NO"))
                .andExpect(jsonPath("$.itens[0].url")
                        .value(Matchers.endsWith("/api/v1/screens/topics/%d/votes".formatted(topicId))));
    }

    @Test
    void recordsTheVoteAndConfirmsItOnAScreen() throws Exception {
        long topicId = topicWithAnOpenSession("Reforma do estatuto");

        screenVote(topicId, "12345678901", "YES")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("FORMULARIO"))
                .andExpect(jsonPath("$.itens[0].tipo").value("TEXTO"))
                .andExpect(jsonPath("$.itens[0].texto").value(Matchers.containsString("registrado")))
                .andExpect(jsonPath("$.botaoOk.url").exists());

        mockMvc.perform(get("/api/v1/topics/%d/result".formatted(topicId)))
                .andExpect(jsonPath("$.yes").value(1));
    }

    @Test
    void explainsARefusedVoteOnAScreenInsteadOfFailing() throws Exception {
        long topicId = topicWithAnOpenSession("Reforma do estatuto");
        screenVote(topicId, "12345678901", "YES").andExpect(status().isOk());

        screenVote(topicId, "12345678901", "NO")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("FORMULARIO"))
                .andExpect(jsonPath("$.itens[0].texto").value(Matchers.containsString("já votou")));
    }

    private ResultActions screenVote(long topicId, String memberId, String choice) throws Exception {
        return mockMvc.perform(post("/api/v1/screens/topics/%d/votes".formatted(topicId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\": \"%s\", \"choice\": \"%s\"}".formatted(memberId, choice)));
    }

    private long topicWithAnOpenSession(String title) throws Exception {
        long topicId = registerTopic(title);
        mockMvc.perform(post("/api/v1/topics/%d/sessions".formatted(topicId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"durationMinutes\": 30}"))
                .andExpect(status().isCreated());
        return topicId;
    }

    private long registerTopic(String title) throws Exception {
        String body = mockMvc.perform(post("/api/v1/topics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"%s\"}".formatted(title)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return Long.parseLong(body.replaceAll(".*\"id\"\\s*:\\s*(\\d+).*", "$1"));
    }
}
