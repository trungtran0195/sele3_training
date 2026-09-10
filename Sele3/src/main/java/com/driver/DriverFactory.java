package com.driver;

import com.config.Configuration;
import com.config.Browser;
import com.driver.browser.BrowserManager;
import org.openqa.selenium.WebDriver;

import java.util.Objects;

public final class DriverFactory {

    public static WebDriver createDriver(Configuration configuration) {
        Objects.requireNonNull(configuration, "Configuration must not be null");
        Browser browser = Objects.requireNonNull(configuration.getBrowser(), "Browser must not be null");

        return BrowserManager.create(browser, configuration);
    }
}
