package com.assertion;

import com.config.Configuration;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.FluentWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Framework-independent immediate and retrying assertions. Create one manager per test so soft
 * assertion failures remain isolated.
 */
public final class AssertionManager {

    private final Duration timeout;
    private final Duration pollingInterval;
    private final List<AssertionError> softFailures = new ArrayList<>();

    public AssertionManager(Configuration configuration) {
        this(
                Objects.requireNonNull(configuration, "Configuration must not be null").getTimeout(),
                configuration.getPollingInterval());
    }

    AssertionManager(Duration timeout, Duration pollingInterval) {
        this.timeout = requirePositive(timeout, "Assertion timeout");
        this.pollingInterval = requirePositive(pollingInterval, "Assertion polling interval");
    }

    /** Checks a condition immediately and stops the test when it is false. */
    public void assertTrue(boolean condition, String message) {
        String validMessage = requireMessage(message);
        if (!condition) {
            throw new AssertionError(validMessage);
        }
    }

    /** Compares two values immediately and stops the test when they differ. */
    public <T> void assertEquals(T actual, T expected, String message) {
        String validMessage = requireMessage(message);
        if (!Objects.equals(actual, expected)) {
            throw new AssertionError(formatMismatch(validMessage, expected, actual));
        }
    }

    /**
     * Retries an immediate condition and stops the test if it never becomes true. The supplied
     * condition should not perform its own wait.
     */
    public void awaitTrue(BooleanSupplier condition, String message) {
        Objects.requireNonNull(condition, "Assertion condition must not be null");
        String validMessage = requireMessage(message);
        retryUntil(condition, () -> validMessage);
    }

    /**
     * Retries an immediate value read until its latest value equals the expected value. The
     * supplied read should not perform its own wait.
     */
    public <T> void awaitEquals(Supplier<T> immediateActualValue, T expected, String message) {
        Objects.requireNonNull(immediateActualValue, "Actual value supplier must not be null");
        String validMessage = requireMessage(message);
        AtomicReference<T> latestValue = new AtomicReference<>();

        retryUntil(
                () -> {
                    T actual = immediateActualValue.get();
                    latestValue.set(actual);
                    return Objects.equals(actual, expected);
                },
                () -> formatMismatch(validMessage, expected, latestValue.get()));
    }

    /** Checks a condition immediately and records the failure without stopping the test. */
    public void softTrue(boolean condition, String message) {
        recordFailure(() -> assertTrue(condition, message));
    }

    /** Compares two values immediately and records the failure without stopping the test. */
    public <T> void softEquals(T actual, T expected, String message) {
        recordFailure(() -> assertEquals(actual, expected, message));
    }

    /** Retries a condition and records a failure only after its timeout. */
    public void softAwaitTrue(BooleanSupplier condition, String message) {
        recordTimeoutFailure(() -> awaitTrue(condition, message));
    }

    /** Retries a value comparison and records a failure only after its timeout. */
    public <T> void softAwaitEquals(Supplier<T> immediateActualValue, T expected, String message) {
        recordTimeoutFailure(() -> awaitEquals(immediateActualValue, expected, message));
    }

    /** Throws all recorded soft assertion failures and clears them from this manager. */
    public void assertAll() {
        if (softFailures.isEmpty()) {
            return;
        }

        List<AssertionError> failures = List.copyOf(softFailures);
        softFailures.clear();
        AssertionError combined = new AssertionError(
                failures.size() + " soft assertion(s) failed");
        failures.forEach(combined::addSuppressed);
        throw combined;
    }

    private void retryUntil(BooleanSupplier condition, Supplier<String> failureMessage) {
        try {
            new FluentWait<>(condition)
                    .withTimeout(timeout)
                    .pollingEvery(pollingInterval)
                    .until(this::evaluate);
        } catch (ConditionTimeoutException e) {
            throw e.timeout;
        } catch (TimeoutException e) {
            throw new AssertionTimeoutException(failureMessage.get(), e);
        }
    }

    private boolean evaluate(BooleanSupplier condition) {
        try {
            return condition.getAsBoolean();
        } catch (TimeoutException e) {
            // A supplier with its own wait has already exhausted that wait. Preserve its failure
            // instead of treating it as the timeout produced by this assertion's FluentWait.
            throw new ConditionTimeoutException(e);
        }
    }

    private void recordFailure(Runnable assertion) {
        try {
            assertion.run();
        } catch (AssertionError failure) {
            softFailures.add(failure);
        }
    }

    private void recordTimeoutFailure(Runnable assertion) {
        try {
            assertion.run();
        } catch (AssertionTimeoutException failure) {
            softFailures.add(failure);
        }
    }

    private static Duration requirePositive(Duration value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be greater than zero");
        }
        return value;
    }

    private static String requireMessage(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Assertion message must not be blank");
        }
        return message;
    }

    private static String formatMismatch(String message, Object expected, Object actual) {
        return message + " expected:<" + expected + "> but was:<" + actual + ">";
    }

    private static final class ConditionTimeoutException extends RuntimeException {

        private final TimeoutException timeout;

        private ConditionTimeoutException(TimeoutException timeout) {
            super(timeout);
            this.timeout = timeout;
        }
    }
}
