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
}
