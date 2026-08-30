package com.driver.browser;

import com.config.Configuration;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;

public final class FirefoxManager {

    private FirefoxManager() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static WebDriver create(Configuration configuration) {
        FirefoxOptions options = new FirefoxOptions();
        options.merge(configuration.getCapabilities());
        options.setPageLoadStrategy(configuration.getPageLoadStrategy());
        if (configuration.isHeadless()) {
            options.addArguments("-headless");
        }

        return configuration.isRemote()
                ? new RemoteWebDriver(remoteUrl(configuration), options)
                : new FirefoxDriver(options);
    }

    private static URL remoteUrl(Configuration configuration) {
        try {
            return new URL(configuration.getRemote());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid remote WebDriver URL: " + configuration.getRemote(), e);
        }
    }
}
