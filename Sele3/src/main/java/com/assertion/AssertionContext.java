package com.assertion;

import com.config.Configuration;

/** Holds the assertion session for the test running on the current thread. */
public final class AssertionContext {

    private static final ThreadLocal<AssertionManager> CURRENT = new ThreadLocal<>();

    private AssertionContext() {
    }

    /** Starts an isolated assertion session for the current test thread. */
    public static void start(Configuration configuration) {
        if (CURRENT.get() != null) {
            throw new IllegalStateException("Assertion session has already started for this thread");
        }
        CURRENT.set(new AssertionManager(configuration));
    }

    /** Returns the assertion manager associated with the current test thread. */
    public static AssertionManager current() {
        AssertionManager assertions = CURRENT.get();
        if (assertions == null) {
            throw new IllegalStateException("Assertion session has not been started for this thread");
        }
        return assertions;
    }

    /** Flushes soft failures and always removes the current thread's assertion session. */
    public static void finish() {
        AssertionManager assertions = CURRENT.get();
        if (assertions == null) {
            return;
        }
        try {
            assertions.assertAll();
        } finally {
            CURRENT.remove();
        }
    }
}
