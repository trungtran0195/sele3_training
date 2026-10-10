package com.assertion;

import com.config.Configuration;
import com.driver.DriverContext;

/** Holds the assertion session for the test running on the current thread. */
public final class AssertionContext {

    private static final ThreadLocal<AssertionManager> CURRENT = new ThreadLocal<>();

    private AssertionContext() {
    }

    /**
     * Returns the assertion manager associated with the current test thread, creating it from the
     * thread's configuration on first use.
     */
    public static AssertionManager current() {
        AssertionManager assertions = CURRENT.get();
        if (assertions == null) {
            Configuration configuration = DriverContext.getConfig();
            assertions = new AssertionManager(configuration);
            CURRENT.set(assertions);
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
