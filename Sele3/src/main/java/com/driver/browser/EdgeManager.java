package com.driver.browser;

import com.config.Configuration;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;

public final class EdgeManager {

    private EdgeManager() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static WebDriver create(Configuration configuration) {
        EdgeOptions options = new EdgeOptions();
        options.merge(configuration.getCapabilities());
        options.setPageLoadStrategy(configuration.getPageLoadStrategy());
        if (configuration.isHeadless()) {
            options.addArguments("--headless=new");
        }

        return configuration.isRemote()
                ? new RemoteWebDriver(remoteUrl(configuration), options)
                : new EdgeDriver(options);
    }

    private static URL remoteUrl(Configuration configuration) {
        try {
            return new URL(configuration.getRemote());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid remote WebDriver URL: " + configuration.getRemote(), e);
        }
    }
}
