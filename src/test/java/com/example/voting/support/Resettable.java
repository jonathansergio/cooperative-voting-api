package com.example.voting.support;

/**
 * A test double that the Spring context shares between every test in a class. Anything shared and
 * mutable has to go back to a known state between tests, and {@link ResetSharedDoubles} does that
 * for every bean of this type so no test has to remember to.
 */
public interface Resettable {

    void reset();
}
