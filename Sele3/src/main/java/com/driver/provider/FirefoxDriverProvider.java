package com.driver.provider;

import com.config.DriverConfig;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

public final class FirefoxDriverProvider extends AbstractBrowserProvider {

    @Override
    public String id() {
        return "firefox";
    }

    @Override
    public WebDriver create(DriverConfig config) {
        FirefoxOptions options = new FirefoxOptions();
        applyCommonOptions(options, config);
        if (config.isHeadless()) {
            options.addArguments("-headless");
        }
        return create(config, options, () -> new FirefoxDriver(options));
    }
}
