package com.assertion;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

public class AssertionManagerTest {

    private static final Duration TIMEOUT = Duration.ofMillis(50);
    private static final Duration POLLING_INTERVAL = Duration.ofMillis(5);

    @Test
    public void shouldFailHardAssertionImmediately() {
        AssertionManager assertions = manager();

        AssertionError failure = Assert.expectThrows(
                AssertionError.class,
                () -> assertions.assertTrue(false, "condition failed"));

        Assert.assertEquals(failure.getMessage(), "condition failed");
    }

    @Test
    public void shouldCompareValuesImmediately() {
        AssertionManager assertions = manager();

        AssertionError failure = Assert.expectThrows(
                AssertionError.class,
                () -> assertions.assertEquals("actual", "expected", "values differ"));

        Assert.assertEquals(
                failure.getMessage(),
                "values differ expected:<expected> but was:<actual>");
    }

    @Test
    public void shouldRetryUntilConditionBecomesTrue() {
        AssertionManager assertions = manager();
        AtomicInteger attempts = new AtomicInteger();

        assertions.awaitTrue(() -> attempts.incrementAndGet() == 3, "condition should become true");

        Assert.assertEquals(attempts.get(), 3);
    }

    @Test
    public void shouldReadLatestValueForEveryEqualsAttempt() {
        AssertionManager assertions = manager();
        AtomicInteger value = new AtomicInteger();

        assertions.awaitEquals(value::incrementAndGet, 3, "value should reach three");

        Assert.assertEquals(value.get(), 3);
    }

    @Test
    public void shouldRetryTransientRuntimeException() {
        AssertionManager assertions = manager();
        AtomicInteger attempts = new AtomicInteger();

        assertions.awaitTrue(() -> {
            if (attempts.incrementAndGet() < 3) {
                throw new IllegalStateException("value is not ready");
            }
            return true;
        }, "condition should recover");

        Assert.assertEquals(attempts.get(), 3);
    }

    @Test
    public void shouldReportLatestActualValueWhenEqualsTimesOut() {
        AssertionManager assertions = manager();

        AssertionError failure = Assert.expectThrows(
                AssertionError.class,
                () -> assertions.awaitEquals(() -> "actual", "expected", "values differ"));

        Assert.assertEquals(
                failure.getMessage(),
                "values differ expected:<expected> but was:<actual>");
    }

    @Test
    public void shouldCollectAndClearSoftFailures() {
        AssertionManager assertions = manager();

        assertions.softTrue(false, "first failure");
        assertions.softEquals("actual", "expected", "second failure");

        AssertionError combined = Assert.expectThrows(AssertionError.class, assertions::assertAll);
        Assert.assertEquals(combined.getSuppressed().length, 2);

        assertions.assertAll();
    }

    @Test
    public void shouldRetrySoftAssertionsBeforeRecordingFailure() {
        AssertionManager assertions = manager();
        AtomicInteger attempts = new AtomicInteger();

        assertions.softAwaitTrue(
                () -> attempts.incrementAndGet() == 3,
                "condition should become true");
        assertions.softAwaitEquals(() -> "actual", "expected", "values differ");

        Assert.assertEquals(attempts.get(), 3);
        AssertionError combined = Assert.expectThrows(AssertionError.class, assertions::assertAll);
        Assert.assertEquals(combined.getSuppressed().length, 1);
    }

    private static AssertionManager manager() {
        return new AssertionManager(TIMEOUT, POLLING_INTERVAL);
    }
}
