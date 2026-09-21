package com.listener;

import com.config.ConfigurationException;
import com.report.ConsoleReporter;
import com.report.ReporterRegistry;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

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
}
