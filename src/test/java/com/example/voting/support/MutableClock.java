package com.example.voting.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * A clock the test moves by hand. Time-dependent behaviour is asserted by advancing it instead of
 * sleeping, which keeps the suite fast and deterministic.
 */
public final class MutableClock extends Clock implements Resettable {

    private final ZoneId zone;
    private final Instant start;
    private Instant instant;

    public MutableClock(Instant start) {
        this(start, ZoneOffset.UTC);
    }

    private MutableClock(Instant start, ZoneId zone) {
        this.start = start;
        this.instant = start;
        this.zone = zone;
    }

    public void advanceBy(Duration amount) {
        instant = instant.plus(amount);
    }

    /**
     * Returns the clock to its starting point. The bean is shared by every test in the context, so a
     * test that moves time must not leave it moved for the next one.
     */
    @Override
    public void reset() {
        instant = start;
    }

    @Override
    public Instant instant() {
        return instant;
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId other) {
        return new MutableClock(instant, other);
    }
}
