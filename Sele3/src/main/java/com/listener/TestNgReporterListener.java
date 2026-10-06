package com.listener;

import com.config.ConfigurationException;
import com.driver.DriverContext;
import com.report.ConsoleReporter;
import com.report.Reporter;
import com.report.ReporterRegistry;
import com.report.TestEvent;
import com.report.TestStatus;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/** Converts TestNG lifecycle callbacks into framework-neutral report events. */
@Slf4j
public class TestNgReporterListener implements ISuiteListener, ITestListener {

    public static final String REPORTERS_PARAMETER = "reporters";
    public static final String REGISTRY_ATTRIBUTE = ReporterRegistry.class.getName();

    private static final Map<String, Supplier<Reporter>> REPORTER_FACTORIES = Map.of(
            "console", ConsoleReporter::new);

    @Override
    public void onStart(ISuite suite) {
        ReporterRegistry registry = new ReporterRegistry();
        registerConfiguredReporters(
                registry,
                resolveConfiguredReporters(suite.getParameter(REPORTERS_PARAMETER)));
        suite.setAttribute(REGISTRY_ATTRIBUTE, registry);
    }

    static String resolveConfiguredReporters(String suiteReporters) {
        return System.getProperty(REPORTERS_PARAMETER, suiteReporters);
    }

    @Override
    public void onTestStart(ITestResult result) {
        publish(result, TestStatus.STARTED, null);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        publish(result, TestStatus.PASSED, null);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        publish(result, TestStatus.FAILED, result.getThrowable());
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        publish(result, TestStatus.SKIPPED, result.getThrowable());
    }

    static void registerConfiguredReporters(ReporterRegistry registry, String configuredReporters) {
        String value = configuredReporters == null || configuredReporters.isBlank()
                ? "console"
                : configuredReporters;

        Set<String> names = new LinkedHashSet<>();
        for (String configuredName : value.split(",")) {
            String name = configuredName.trim().toLowerCase(Locale.ROOT);
            if (!names.add(name)) {
                continue;
            }
            Supplier<Reporter> factory = REPORTER_FACTORIES.get(name);
            if (factory == null) {
                throw new ConfigurationException("Unsupported reporter: " + configuredName.trim());
            }
            registry.register(factory.get());
        }
    }

    private static void publish(ITestResult result, TestStatus status, Throwable error) {
        ISuite suite = result.getTestContext().getSuite();
        Object value = suite.getAttribute(REGISTRY_ATTRIBUTE);
        if (!(value instanceof ReporterRegistry registry)) {
            throw new IllegalStateException("Reporter registry has not been initialized");
        }

        publish(registry, result.getMethod().getQualifiedName(), status, error);
    }

    static void publish(
            ReporterRegistry registry,
            String testName,
            TestStatus status,
            Throwable error) {
        TestEvent event = new TestEvent(testName, status, error);
        registry.publish(event);

        if (status == TestStatus.FAILED) {
            byte[] screenshot = captureScreenshot();
            if (screenshot != null) {
                registry.attachScreenshot(event, screenshot);
            }
        }
    }

    private static byte[] captureScreenshot() {
        WebDriver driver = DriverContext.getDriver();
        if (!(driver instanceof TakesScreenshot screenshotDriver)) {
            return null;
        }
        try {
            return screenshotDriver.getScreenshotAs(OutputType.BYTES);
        } catch (RuntimeException e) {
            log.warn("Could not capture screenshot for failed test", e);
            return null;
        }
    }
}
