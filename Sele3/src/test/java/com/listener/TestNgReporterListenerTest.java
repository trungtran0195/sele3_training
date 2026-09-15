package com.listener;

import com.config.ConfigurationException;
import com.report.ConsoleReporter;
import com.report.ReporterRegistry;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestNgReporterListenerTest {

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

    @Test(expectedExceptions = ConfigurationException.class)
    public void shouldRejectUnsupportedReporter() {
        TestNgReporterListener.registerConfiguredReporters(
                new ReporterRegistry(),
                "unknown");
    }
}
