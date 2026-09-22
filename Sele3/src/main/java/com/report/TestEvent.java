package com.report;

import java.time.Instant;
import java.util.Objects;

/** Immutable event passed from a test integration to registered reporters. */
public record TestEvent(
        String testName,
        TestStatus status,
        Throwable error,
        Instant timestamp) {

    public TestEvent {
        if (testName == null || testName.isBlank()) {
            throw new IllegalArgumentException("Test name must not be blank");
        }
        Objects.requireNonNull(status, "Test status must not be null");
        Objects.requireNonNull(timestamp, "Event timestamp must not be null");
    }

    public TestEvent(String testName, TestStatus status, Throwable error) {
        this(testName, status, error, Instant.now());
    }
}
