package com.listener;

import com.config.ConfigurationException;
import com.driver.DriverContext;
import com.report.ConsoleReporter;
import com.report.Reporter;
import com.report.ReporterRegistry;
import com.report.TestEvent;
import com.report.TestStatus;
import org.mockito.MockedStatic;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

public class TestNgReporterListenerTest {

    private String previousReportersProperty;

    @BeforeMethod
    public void rememberReportersProperty() {
        previousReportersProperty = System.getProperty(TestNgReporterListener.REPORTERS_PARAMETER);
    }

    @AfterMethod
    public void restoreReportersProperty() {
        if (previousReportersProperty == null) {
            System.clearProperty(TestNgReporterListener.REPORTERS_PARAMETER);
        } else {
            System.setProperty(
                    TestNgReporterListener.REPORTERS_PARAMETER,
                    previousReportersProperty);
        }
    }

    @Test
    public void shouldRegisterConsoleReporterByDefault() {
        ReporterRegistry registry = new ReporterRegistry();

        TestNgReporterListener.registerConfiguredReporters(registry, null);

        Assert.assertEquals(registry.registeredReporters().size(), 1);
        Assert.assertEquals(registry.registeredReporters().get(0).getClass(), ConsoleReporter.class);
    }

    @Test
    public void shouldIgnoreDuplicateReporterNames() {
        ReporterRegistry registry = new ReporterRegistry();

        TestNgReporterListener.registerConfiguredReporters(registry, "console, CONSOLE");

        Assert.assertEquals(registry.registeredReporters().size(), 1);
    }

    @Test
    public void shouldUseSystemPropertyInsteadOfSuiteValue() {
        System.setProperty(TestNgReporterListener.REPORTERS_PARAMETER, "system-reporter");

        Assert.assertEquals(
                TestNgReporterListener.resolveConfiguredReporters("suite-reporter"),
                "system-reporter");
    }

    @Test
    public void shouldUseSuiteValueWhenSystemPropertyIsMissing() {
        System.clearProperty(TestNgReporterListener.REPORTERS_PARAMETER);

        Assert.assertEquals(
                TestNgReporterListener.resolveConfiguredReporters("suite-reporter"),
                "suite-reporter");
    }

    @Test(expectedExceptions = ConfigurationException.class)
    public void shouldRejectUnsupportedReporter() {
        TestNgReporterListener.registerConfiguredReporters(
                new ReporterRegistry(),
                "unknown");
    }

    @Test
    public void shouldAttachScreenshotWhenTestFails() {
        ReporterRegistry registry = new ReporterRegistry();
        RecordingReporter reporter = new RecordingReporter();
        registry.register(reporter);
        AssertionError failure = new AssertionError("failed");
        WebDriver driver = mock(
                WebDriver.class,
                withSettings().extraInterfaces(TakesScreenshot.class));
        byte[] screenshot = {1, 2, 3};
        when(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES))
                .thenReturn(screenshot);

        try (MockedStatic<DriverContext> context = mockStatic(DriverContext.class)) {
            context.when(DriverContext::getDriver).thenReturn(driver);
            TestNgReporterListener.publish(
                    registry,
                    "tests.CheckoutTest.checkout",
                    TestStatus.FAILED,
                    failure);
        }

        Assert.assertSame(reporter.event.error(), failure);
        Assert.assertEquals(reporter.screenshot, screenshot);
    }

    @Test
    public void shouldKeepOriginalFailureWhenScreenshotCannotBeCaptured() {
        ReporterRegistry registry = new ReporterRegistry();
        RecordingReporter reporter = new RecordingReporter();
        registry.register(reporter);
        AssertionError failure = new AssertionError("failed");
        WebDriver driver = mock(
                WebDriver.class,
                withSettings().extraInterfaces(TakesScreenshot.class));
        when(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES))
                .thenThrow(new IllegalStateException("driver closed"));

        try (MockedStatic<DriverContext> context = mockStatic(DriverContext.class)) {
            context.when(DriverContext::getDriver).thenReturn(driver);
            TestNgReporterListener.publish(
                    registry,
                    "tests.CheckoutTest.checkout",
                    TestStatus.FAILED,
                    failure);
        }

        Assert.assertSame(reporter.event.error(), failure);
        Assert.assertNull(reporter.screenshot);
    }

    private static final class RecordingReporter implements Reporter {

        private TestEvent event;
        private byte[] screenshot;

        @Override
        public void report(TestEvent event) {
            this.event = event;
        }

        @Override
        public void attachScreenshot(TestEvent event, byte[] screenshot) {
            this.screenshot = screenshot;
        }
    }
}
