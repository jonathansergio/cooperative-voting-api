package com.example.voting.integration.userinfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.voting.TestcontainersConfiguration;
import com.example.voting.support.TestTimeConfiguration;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Runs the real eligibility client — not the test double — against a registry that answers far too
 * late. Every other eligibility test replaces the transport, so this is the one that proves the
 * timeouts are actually wired into the client, and that a slow upstream cuts a vote short with a 503
 * instead of holding the request for as long as the upstream feels like.
 */
@SpringBootTest(
        properties = {
            "voting.eligibility.mode=remote",
            "voting.eligibility.connect-timeout=500ms",
            "voting.eligibility.read-timeout=300ms"
        })
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestTimeConfiguration.class})
class SlowRegistryTest {

    private static final Duration REGISTRY_DELAY = Duration.ofSeconds(2);
    private static final AtomicInteger lookups = new AtomicInteger();
    private static final HttpServer registry = startSlowRegistry();

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void pointAtTheSlowRegistry(DynamicPropertyRegistry properties) {
        properties.add(
                "voting.eligibility.base-url",
                () -> "http://localhost:" + registry.getAddress().getPort());
    }

    @AfterAll
    static void stopTheRegistry() {
        registry.stop(0);
    }

    @Test
    void answersUnavailableQuicklyInsteadOfWaitingForASlowRegistry() throws Exception {
        long topicId = topicWithAnOpenSession();
        long startedAt = System.nanoTime();

        mockMvc.perform(post("/api/v1/topics/%d/votes".formatted(topicId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\": \"12345678901\", \"choice\": \"YES\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));

        Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);
        assertThat(lookups.get()).as("the vote really consulted the registry").isPositive();
        assertThat(elapsed)
                .as("the read timeout cut the call off long before the registry would have answered")
                .isLessThan(REGISTRY_DELAY.dividedBy(2));
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

    private static HttpServer startSlowRegistry() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/users/", exchange -> {
                lookups.incrementAndGet();
                try {
                    Thread.sleep(REGISTRY_DELAY);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
                byte[] answer = "{\"status\": \"ABLE_TO_VOTE\"}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, answer.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(answer);
                }
            });
            server.start();
            return server;
        } catch (IOException unableToStart) {
            throw new UncheckedIOException(unableToStart);
        }
    }
}
