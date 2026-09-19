package com.report;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/** Thread-safe collection of reporters registered by framework setup code. */
public final class ReporterRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReporterRegistry.class);

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
                LOGGER.warn(
                        "Reporter {} failed while handling {} for test {}",
                        reporter.getClass().getName(),
                        event.status(),
                        event.testName(),
                        e);
            }
        }
    }
}
