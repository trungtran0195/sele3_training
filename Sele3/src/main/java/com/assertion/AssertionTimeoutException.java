package com.assertion;

/** Indicates that a retrying assertion did not match before its own timeout expired. */
public final class AssertionTimeoutException extends AssertionError {

    public AssertionTimeoutException(String message) {
        super(message);
    }
}
