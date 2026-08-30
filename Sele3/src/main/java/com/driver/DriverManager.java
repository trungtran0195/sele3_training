package com.driver;

import com.config.Configuration;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;

import java.util.Objects;

public final class DriverManager {

    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();
    private static final ThreadLocal<Configuration> CONFIG = new ThreadLocal<>();

    private DriverManager() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void setConfig(Configuration configuration) {
        Objects.requireNonNull(configuration, "Configuration must not be null");
        if (DRIVER.get() != null) {
            throw new IllegalStateException("Cannot replace configuration while a driver is active");
        }
        CONFIG.set(new Configuration(configuration));
    }

    public static Configuration getConfig() {
        Configuration configuration = CONFIG.get();
        if (configuration == null) {
            throw new IllegalStateException("Configuration has not been initialized for this thread");
        }
        return configuration;
    }

    public static void createDriver() {
        if (DRIVER.get() != null) {
            throw new IllegalStateException("A WebDriver is already active for this thread");
        }

        Configuration configuration = getConfig();
        WebDriver webDriver = null;
        try {
            webDriver = DriverFactory.createDriver(configuration);
            configureSession(webDriver, configuration);
            DRIVER.set(webDriver);
        } catch (RuntimeException e) {
            if (webDriver != null) {
                try {
                    webDriver.quit();
                } catch (RuntimeException quitError) {
                    e.addSuppressed(quitError);
                }
            }
            throw new IllegalStateException("Unable to create WebDriver for browser: "
                    + configuration.getBrowser(), e);
        }
    }

    public static WebDriver getDriver() {
        WebDriver webDriver = DRIVER.get();
        if (webDriver == null) {
            throw new IllegalStateException("WebDriver has not been initialized for this thread");
        }
        return webDriver;
    }

    public static void quitDriver() {
        WebDriver webDriver = DRIVER.get();
        try {
            if (webDriver != null) {
                webDriver.quit();
            }
        } finally {
            DRIVER.remove();
        }
    }

    public static void cleanup() {
        try {
            quitDriver();
        } finally {
            CONFIG.remove();
        }
    }

    private static void configureSession(WebDriver webDriver, Configuration configuration) {
        webDriver.manage().timeouts().implicitlyWait(java.time.Duration.ZERO);
        webDriver.manage().timeouts().pageLoadTimeout(configuration.getPageLoadTimeout());

        if (configuration.isStartMaximized() && !configuration.isHeadless()) {
            webDriver.manage().window().maximize();
            return;
        }

        String[] size = configuration.getBrowserSize().toLowerCase().split("x", 2);
        webDriver.manage().window().setSize(new Dimension(
                Integer.parseInt(size[0]),
                Integer.parseInt(size[1])));
    }
}
