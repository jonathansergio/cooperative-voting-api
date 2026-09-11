package com.example.voting.vote;

import static org.assertj.core.api.Assertions.assertThat;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.LongRange;

/**
 * The outcome of a tally, stated as rules that hold for any count rather than for a few chosen ones.
 * The three together specify the function completely.
 */
class OutcomePropertiesTest {

    @Property
    void approvesExactlyWhenYesOutnumbersNo(
            @ForAll @LongRange(max = 1_000_000) long yes, @ForAll @LongRange(max = 1_000_000) long no) {
        assertThat(Outcome.of(yes, no) == Outcome.APPROVED).isEqualTo(yes > no);
    }

    @Property
    void rejectsExactlyWhenNoOutnumbersYes(
            @ForAll @LongRange(max = 1_000_000) long yes, @ForAll @LongRange(max = 1_000_000) long no) {
        assertThat(Outcome.of(yes, no) == Outcome.REJECTED).isEqualTo(yes < no);
    }

    @Property
    void tiesExactlyWhenBothSidesAreEqual(
            @ForAll @LongRange(max = 1_000_000) long yes, @ForAll @LongRange(max = 1_000_000) long no) {
        assertThat(Outcome.of(yes, no) == Outcome.TIED).isEqualTo(yes == no);
    }
}
