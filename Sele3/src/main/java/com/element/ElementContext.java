package com.element;

import com.config.Configuration;
import com.driver.DriverContext;
import org.openqa.selenium.WebDriver;

import java.util.Objects;
import java.util.function.Supplier;

/** Provides the thread-bound driver and configuration used by an element operation. */
final class ElementContext {

    private final Supplier<WebDriver> driverSupplier;
    private final Supplier<Configuration> configurationSupplier;

    private ElementContext(
            Supplier<WebDriver> driverSupplier,
            Supplier<Configuration> configurationSupplier) {
        this.driverSupplier = Objects.requireNonNull(driverSupplier, "Driver supplier must not be null");
        this.configurationSupplier = Objects.requireNonNull(
                configurationSupplier,
                "Configuration supplier must not be null");
    }

    static ElementContext currentThread() {
        return new ElementContext(DriverContext::getDriver, DriverContext::getConfig);
    }

    static ElementContext of(
            Supplier<WebDriver> driverSupplier,
            Supplier<Configuration> configurationSupplier) {
        return new ElementContext(driverSupplier, configurationSupplier);
    }

    WebDriver driver() {
        WebDriver driver = driverSupplier.get();
        if (driver == null) {
            throw new IllegalStateException("WebDriver has not been initialized for this thread");
        }
        return driver;
    }

    Configuration configuration() {
        Configuration configuration = configurationSupplier.get();
        if (configuration == null) {
            throw new IllegalStateException("Configuration has not been initialized for this thread");
        }
        return configuration;
    }
}
