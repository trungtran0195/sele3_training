package com.report;

import lombok.extern.slf4j.Slf4j;

/** Writes test lifecycle events to the configured SLF4J logger. */
@Slf4j
public class ConsoleReporter implements Reporter {

    @Override
    public void report(TestEvent event) {
        switch (event.status()) {
            case STARTED -> log.info("Test started: {}", event.testName());
            case PASSED -> log.info("Test passed: {}", event.testName());
            case SKIPPED -> log.info("Test skipped: {}", event.testName());
            case FAILED -> log.error("Test failed: {}", event.testName(), event.error());
        }
    }
}
