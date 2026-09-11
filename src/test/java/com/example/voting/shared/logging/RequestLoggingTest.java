package com.example.voting.shared.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.voting.support.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Every log line produced while serving a request carries the same correlation id, and the caller
 * gets that id back. Without it, reading the log of a system serving many members at once means
 * guessing which lines belong together.
 */
@IntegrationTest
class RequestLoggingTest {

    private static final String HEADER = "X-Correlation-Id";

    /** Any endpoint serves: these tests are about the logging, not about what the endpoint answers. */
    private static final String SOME_PATH = "/api/v1/topics/1";

    @Autowired
    private MockMvc mockMvc;

    private ListAppender<ILoggingEvent> captured;

    @BeforeEach
    void captureLogs() {
        captured = new ListAppender<>();
        captured.start();
        rootLogger().addAppender(captured);
    }

    @AfterEach
    void stopCapturingLogs() {
        rootLogger().detachAppender(captured);
    }

    @Test
    void answersWithACorrelationIdEvenWhenTheCallerDidNotSendOne() throws Exception {
        mockMvc.perform(get(SOME_PATH))
                .andExpect(header().exists(HEADER))
                .andExpect(header().string(HEADER, matchesPattern("[0-9a-f-]{36}")));
    }

    @Test
    void keepsTheCorrelationIdTheCallerSupplied() throws Exception {
        mockMvc.perform(get(SOME_PATH).header(HEADER, "trace-from-the-caller"))
                .andExpect(header().string(HEADER, "trace-from-the-caller"));
    }

    @Test
    void stampsThatCorrelationIdOnTheLogLinesOfTheRequest() throws Exception {
        mockMvc.perform(get(SOME_PATH).header(HEADER, "a-known-trace"));

        assertThat(captured.list)
                .as("log lines carrying the caller's correlation id")
                .anyMatch(event ->
                        "a-known-trace".equals(event.getMDCPropertyMap().get("correlationId")));
    }

    @Test
    void recordsHowEachRequestEnded() throws Exception {
        int status = mockMvc.perform(get(SOME_PATH)).andReturn().getResponse().getStatus();

        assertThat(captured.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .as("a line describing the request that was just served")
                .anyMatch(message -> message.contains("GET")
                        && message.contains(SOME_PATH)
                        && message.contains(String.valueOf(status)));
    }

    @Test
    void staysQuietAboutHealthProbes() throws Exception {
        mockMvc.perform(get("/actuator/health"));

        assertThat(captured.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .as("health probes are polled constantly and must not fill the log")
                .noneMatch(message -> message.contains("/actuator/health"));
    }

    @Test
    void reportsHowLongTheRequestTookAsAPlausibleNumberOfMilliseconds() throws Exception {
        mockMvc.perform(get(SOME_PATH));

        assertThat(captured.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .as("the request line ends with a real duration, not a number from broken arithmetic")
                .anyMatch(message -> message.contains(SOME_PATH) && message.matches(".* in \\d{1,4}ms$"));
    }

    private Logger rootLogger() {
        return (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    }
}
