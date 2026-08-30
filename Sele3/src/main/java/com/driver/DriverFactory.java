package com.driver;

import com.config.Configuration;
import com.driver.browser.Browser;
import com.driver.browser.ChromeManager;
import com.driver.browser.EdgeManager;
import com.driver.browser.FirefoxManager;
import org.openqa.selenium.WebDriver;

import java.util.Objects;

public final class DriverFactory {

    private DriverFactory() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static WebDriver createDriver(Configuration configuration) {
        Objects.requireNonNull(configuration, "Configuration must not be null");
        Browser browser = Objects.requireNonNull(configuration.getBrowser(), "Browser must not be null");

        return switch (browser) {
            case CHROME -> ChromeManager.create(configuration);
            case FIREFOX -> FirefoxManager.create(configuration);
            case EDGE -> EdgeManager.create(configuration);
        };
    }
}
