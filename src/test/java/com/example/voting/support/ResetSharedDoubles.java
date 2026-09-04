package com.example.voting.support;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Puts every shared test double back to its starting state before each test. Without this, a test
 * that moves the clock or programs an answer leaves it that way for whatever runs next, and the
 * suite starts passing or failing depending on the order it happens to run in.
 */
public final class ResetSharedDoubles implements BeforeEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        SpringExtension.getApplicationContext(context)
                .getBeansOfType(Resettable.class)
                .values()
                .forEach(Resettable::reset);
    }
}
