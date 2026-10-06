package com.report;

/** Receives framework-neutral test events. */
@FunctionalInterface
public interface Reporter {

    /**
     * Handles one test lifecycle event.
     *
     * @param event immutable test event
     * @throws Exception when the reporter cannot process the event
     */
    void report(TestEvent event) throws Exception;

    /**
     * Attaches a failure screenshot to a test event when this reporter supports attachments.
     * Reporters that do not support screenshots can keep the default no-op behavior.
     */
    default void attachScreenshot(TestEvent event, byte[] screenshot) throws Exception {
    }
}
