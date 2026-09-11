package com.example.voting.session;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;

/**
 * The session window, stated for any duration and any moment. Random moments almost never land on
 * the exact instant the session closes, which is precisely where "before" and "at or before" differ,
 * so the second property only draws moments a few seconds around the close.
 */
class VotingSessionWindowPropertiesTest {

    private static final Instant OPENED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Property
    void acceptsVotesExactlyWhileTheDurationHasNotElapsed(
            @ForAll @IntRange(min = 1, max = 24 * 60) int durationMinutes,
            @ForAll @LongRange(max = 2 * 24 * 60 * 60) long elapsedSeconds) {
        VotingSession session = new VotingSession(1L, OPENED_AT, Duration.ofMinutes(durationMinutes));

        assertThat(session.isOpenAt(OPENED_AT.plusSeconds(elapsedSeconds)))
                .isEqualTo(elapsedSeconds < durationMinutes * 60L);
    }

    @Property
    void closesAtTheExactInstantTheDurationEnds(
            @ForAll @IntRange(min = 1, max = 24 * 60) int durationMinutes,
            @ForAll @IntRange(min = -3, max = 3) int secondsAroundTheClose) {
        VotingSession session = new VotingSession(1L, OPENED_AT, Duration.ofMinutes(durationMinutes));
        Instant closesAt = OPENED_AT.plus(Duration.ofMinutes(durationMinutes));

        assertThat(session.isOpenAt(closesAt.plusSeconds(secondsAroundTheClose)))
                .isEqualTo(secondsAroundTheClose < 0);
    }
}
