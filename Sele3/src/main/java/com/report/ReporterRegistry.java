package com.report;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/** Thread-safe collection of reporters registered by framework setup code. */
@Slf4j
public final class ReporterRegistry {

    private final CopyOnWriteArrayList<Reporter> reporters = new CopyOnWriteArrayList<>();

    public void register(Reporter reporter) {
        reporters.addIfAbsent(Objects.requireNonNull(reporter, "Reporter must not be null"));
    }

    public boolean unregister(Reporter reporter) {
        return reporters.remove(reporter);
    }

    public List<Reporter> registeredReporters() {
        return List.copyOf(reporters);
    }

    public void publish(TestEvent event) {
        Objects.requireNonNull(event, "Test event must not be null");
        notifyReporters(
                event,
                reporter -> reporter.report(event),
                "while handling " + event.status());
    }

    public void attachScreenshot(TestEvent event, byte[] screenshot) {
        Objects.requireNonNull(event, "Test event must not be null");
        Objects.requireNonNull(screenshot, "Screenshot must not be null");
        notifyReporters(
                event,
                reporter -> reporter.attachScreenshot(event, screenshot.clone()),
                "to attach screenshot");
    }

    private void notifyReporters(
            TestEvent event,
            ReporterOperation operation,
            String action) {
        for (Reporter reporter : reporters) {
            try {
                operation.accept(reporter);
            } catch (Exception e) {
                log.warn(
                        "Reporter {} failed {} for test {}",
                        reporter.getClass().getName(),
                        action,
                        event.testName(),
                        e);
            }
        }
    }

    @FunctionalInterface
    private interface ReporterOperation {
        void accept(Reporter reporter) throws Exception;
    }
}
