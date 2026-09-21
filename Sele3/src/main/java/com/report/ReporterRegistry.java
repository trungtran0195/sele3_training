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
        for (Reporter reporter : reporters) {
            try {
                reporter.report(event);
            } catch (Exception e) {
                log.warn(
                        "Reporter {} failed while handling {} for test {}",
                        reporter.getClass().getName(),
                        event.status(),
                        event.testName(),
                        e);
            }
        }
    }
}
