package com.assertion;

import com.config.Configuration;

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

    /** Retries the condition and stops the test if it never becomes true. */
    public void awaitTrue(BooleanSupplier condition, String message) {
        Objects.requireNonNull(condition, "Assertion condition must not be null");
        retryUntil(condition, requireMessage(message));
    }

    /** Retries the value supplier until its latest value equals the expected value. */
    public <T> void awaitEquals(Supplier<T> actualValue, T expected, String message) {
        Objects.requireNonNull(actualValue, "Actual value supplier must not be null");
        String validMessage = requireMessage(message);
        AtomicReference<T> latestValue = new AtomicReference<>();

        retryUntil(
                () -> {
                    T actual = actualValue.get();
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
        recordFailure(() -> awaitTrue(condition, message));
    }

    /** Retries a value comparison and records a failure only after its timeout. */
    public <T> void softAwaitEquals(Supplier<T> actualValue, T expected, String message) {
        recordFailure(() -> awaitEquals(actualValue, expected, message));
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

    private void retryUntil(BooleanSupplier condition, String message) {
        retryUntil(condition, () -> message);
    }

    private void retryUntil(BooleanSupplier condition, Supplier<String> failureMessage) {
        long timeoutNanos = timeout.toNanos();
        long startedAt = System.nanoTime();
        RuntimeException lastException = null;

        while (true) {
            try {
                if (condition.getAsBoolean()) {
                    return;
                }
            } catch (RuntimeException e) {
                lastException = e;
            }

            long remainingNanos = timeoutNanos - (System.nanoTime() - startedAt);
            if (remainingNanos <= 0) {
                AssertionError failure = new AssertionError(failureMessage.get());
                if (lastException != null) {
                    failure.initCause(lastException);
                }
                throw failure;
            }

            pause(Math.min(pollingInterval.toNanos(), remainingNanos));
        }
    }

    private void recordFailure(Runnable assertion) {
        try {
            assertion.run();
        } catch (AssertionError failure) {
            softFailures.add(failure);
        }
    }

    private static void pause(long nanos) {
        try {
            Thread.sleep(nanos / 1_000_000, (int) (nanos % 1_000_000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Assertion retry was interrupted", e);
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
}
