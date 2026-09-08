package com.listener;

import com.config.Configuration;
import com.config.ConfigurationLoader;
import com.driver.DriverManager;
import com.report.Reporter;
import com.report.ReporterRegistry;
import com.report.TestEvent;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ReporterListener {

    private final ReporterRegistry registry = new ReporterRegistry();
    private final ThreadLocal<List<Reporter>> currentReporters = new ThreadLocal<>();
    private final Set<Reporter> createdReporters = ConcurrentHashMap.newKeySet();

    public void onStart(TestEvent event) {
        List<Reporter> reporters = registry.create(configuration().getReport());
        currentReporters.set(reporters);
        createdReporters.addAll(reporters);
        reporters.forEach(reporter -> reporter.onStart(event));
    }

    public void onSuccess(TestEvent event) {
        reporters().forEach(reporter -> reporter.onSuccess(event));
        currentReporters.remove();
    }

    public void onFailure(TestEvent event) {
        List<Reporter> reporters = reporters();
        reporters.forEach(reporter -> reporter.onFailure(event));
        attachScreenshot(reporters);
        currentReporters.remove();
    }

    public void onSkipped(TestEvent event) {
        reporters().forEach(reporter -> reporter.onSkipped(event));
        currentReporters.remove();
    }

    public void flush() {
        createdReporters.forEach(Reporter::flush);
        createdReporters.clear();
    }

    private Configuration configuration() {
        Configuration configuration = DriverManager.getConfig();
        return configuration == null ? ConfigurationLoader.load() : configuration;
    }

    private List<Reporter> reporters() {
        List<Reporter> reporters = currentReporters.get();
        return reporters == null ? List.of() : reporters;
    }

    private void attachScreenshot(List<Reporter> reporters) {
        Configuration configuration = configuration();
        if (!configuration.getReport().isScreenshotOnFailure()) {
            return;
        }

        WebDriver driver = DriverManager.getDriver();
        if (driver instanceof TakesScreenshot screenshotDriver) {
            byte[] screenshot = screenshotDriver.getScreenshotAs(OutputType.BYTES);
            reporters.forEach(reporter ->
                    reporter.attach("Failure screenshot", "image/png", screenshot));
        }
    }
}
