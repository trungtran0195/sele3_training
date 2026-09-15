package com.driver.provider;

import com.config.Configuration;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

public class FirefoxDriverProvider extends AbstractDriverProvider<FirefoxOptions> {

    @Override
    public String name() {
        return "firefox";
    }

    @Override
    protected FirefoxOptions createOptions(Configuration configuration) {
        FirefoxOptions options = new FirefoxOptions();
        if (configuration.isHeadless()) {
            options.addArguments("-headless");
        }
        return options;
    }

    @Override
    protected WebDriver createLocal(FirefoxOptions options) {
        return new FirefoxDriver(options);
    }
}
