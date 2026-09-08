package com.driver;

import com.config.Configuration;
import com.config.DriverConfig;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;

import java.util.Objects;
import java.util.logging.Logger;

public final class DriverManager {

    private static final Logger LOGGER = Logger.getLogger(DriverManager.class.getName());
    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();
    private static final ThreadLocal<Configuration> CONFIG = new ThreadLocal<>();

    public static void setConfig(Configuration configuration) {
        Objects.requireNonNull(configuration, "Configuration must not be null");
        if (DRIVER.get() != null) {
            throw new IllegalStateException("Cannot replace configuration while a driver is active");
        }
        CONFIG.set(configuration);
    }

    public static Configuration getConfig() {
        return CONFIG.get();
    }

    public static void createDriver() {
        if (DRIVER.get() != null) {
            LOGGER.warning("A WebDriver is already active for this thread");
            return;
        }

        Configuration configuration = getConfig();
        if (configuration == null) {
            LOGGER.severe("Configuration has not been initialized for this thread");
            return;
        }

        WebDriver webDriver = DriverFactory.createDriver(configuration);
        DRIVER.set(webDriver);
        configureSession(webDriver, configuration.getDriver());
    }

    public static WebDriver getDriver() {
        return DRIVER.get();
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

    private static void configureSession(WebDriver webDriver, DriverConfig configuration) {
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
