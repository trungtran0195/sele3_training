package com.report;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class ReporterRegistryTest {

    @Test
    public void shouldPublishToEveryRegisteredReporter() {
        ReporterRegistry registry = new ReporterRegistry();
        AtomicInteger calls = new AtomicInteger();
        Reporter reporter = event -> calls.incrementAndGet();

        registry.register(reporter);
        registry.register(reporter);
        registry.register(event -> calls.incrementAndGet());
        registry.publish(new TestEvent("login", TestStatus.PASSED, null));

        Assert.assertEquals(calls.get(), 2);
    }

    @Test
    public void shouldContinueWhenAReporterFails() {
        ReporterRegistry registry = new ReporterRegistry();
        AtomicInteger calls = new AtomicInteger();
        registry.register(event -> {
            throw new IllegalStateException("Report failure");
        });
        registry.register(event -> calls.incrementAndGet());

        registry.publish(new TestEvent("checkout", TestStatus.FAILED, new AssertionError()));

        Assert.assertEquals(calls.get(), 1);
    }

    @Test
    public void shouldStopPublishingAfterReporterIsUnregistered() {
        ReporterRegistry registry = new ReporterRegistry();
        AtomicInteger calls = new AtomicInteger();
        Reporter reporter = event -> calls.incrementAndGet();
        registry.register(reporter);

        Assert.assertTrue(registry.unregister(reporter));
        registry.publish(new TestEvent("search", TestStatus.SKIPPED, null));

        Assert.assertEquals(calls.get(), 0);
        Assert.assertTrue(registry.registeredReporters().isEmpty());
    }
}
