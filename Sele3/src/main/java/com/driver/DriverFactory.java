package com.driver;

import com.config.Configuration;
import com.config.DriverConfig;
import org.openqa.selenium.WebDriver;

import java.util.Objects;

public final class DriverFactory {

    private static final DriverRegistry REGISTRY = new DriverRegistry();

    public static WebDriver createDriver(Configuration configuration) {
        Objects.requireNonNull(configuration, "Configuration must not be null");
        DriverConfig driverConfig = Objects.requireNonNull(
                configuration.getDriver(), "Driver configuration must not be null");
        return REGISTRY.create(driverConfig);
    }
}
