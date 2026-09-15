package com.report;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Writes test lifecycle events to the configured SLF4J logger. */
public class ConsoleReporter implements Reporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleReporter.class);

    @Override
    public void report(TestEvent event) {
        switch (event.status()) {
            case STARTED -> LOGGER.info("Test started: {}", event.testName());
            case PASSED -> LOGGER.info("Test passed: {}", event.testName());
            case SKIPPED -> LOGGER.info("Test skipped: {}", event.testName());
            case FAILED -> LOGGER.error("Test failed: {}", event.testName(), event.error());
        }
    }
}
