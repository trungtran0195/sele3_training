package com.driver;

import com.config.Configuration;
import org.openqa.selenium.WebDriver;

import java.util.Objects;

public final class DriverFactory {

    public static WebDriver createDriver(Configuration configuration) {
        Objects.requireNonNull(configuration, "Configuration must not be null");
        Objects.requireNonNull(configuration.getBrowser(), "Browser must not be null");
        return DriverRegistry.create(configuration);
    }
}
