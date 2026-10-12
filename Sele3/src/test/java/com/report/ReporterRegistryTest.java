package com.report;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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

    @Test
    public void shouldAttachScreenshotToEveryReporter() {
        ReporterRegistry registry = new ReporterRegistry();
        AtomicReference<byte[]> received = new AtomicReference<>();
        registry.register(new Reporter() {
            @Override
            public void report(TestEvent event) {
            }

            @Override
            public void attachScreenshot(TestEvent event, byte[] screenshot) {
                received.set(screenshot);
            }
        });
        byte[] screenshot = {1, 2, 3};

        registry.attachScreenshot(
                new TestEvent("checkout", TestStatus.FAILED, new AssertionError()),
                screenshot);

        Assert.assertEquals(received.get(), screenshot);
        Assert.assertNotSame(received.get(), screenshot);
    }

    @Test
    public void shouldContinueWhenAReporterCannotAttachScreenshot() {
        ReporterRegistry registry = new ReporterRegistry();
        AtomicInteger attachments = new AtomicInteger();
        registry.register(new ScreenshotReporter(() -> {
            throw new IllegalStateException("Attachment failure");
        }));
        registry.register(new ScreenshotReporter(attachments::incrementAndGet));

        registry.attachScreenshot(
                new TestEvent("checkout", TestStatus.FAILED, new AssertionError()),
                new byte[]{1});

        Assert.assertEquals(attachments.get(), 1);
    }

    private static final class ScreenshotReporter implements Reporter {

        private final Runnable attachment;

        private ScreenshotReporter(Runnable attachment) {
            this.attachment = attachment;
        }

        @Override
        public void report(TestEvent event) {
        }

        @Override
        public void attachScreenshot(TestEvent event, byte[] screenshot) {
            attachment.run();
        }
    }
}
