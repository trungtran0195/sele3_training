package com.driver.browser;

import com.config.Browser;
import com.config.Configuration;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.AbstractDriverOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.Function;

public final class BrowserManager {

    private static final Map<Browser, Function<Configuration, DriverPlan>> BROWSERS = Map.of(
            Browser.CHROME, BrowserManager::chrome,
            Browser.FIREFOX, BrowserManager::firefox,
            Browser.EDGE, BrowserManager::edge);

    public static WebDriver create(Browser browser, Configuration config) {
        DriverPlan plan = BROWSERS.get(browser).apply(config);
        return config.isRemote()
                ? new RemoteWebDriver(remoteUrl(config), plan.options())
                : plan.localDriver().get();
    }

    private static DriverPlan chrome(Configuration config) {
        ChromeOptions options = new ChromeOptions();
        configure(options, config);
        if (config.isHeadless()) {
            options.addArguments("--headless=new");
        }
        return new DriverPlan(options, () -> new ChromeDriver(options));
    }

    private static DriverPlan firefox(Configuration config) {
        FirefoxOptions options = new FirefoxOptions();
        configure(options, config);
        if (config.isHeadless()) {
            options.addArguments("-headless");
        }
        return new DriverPlan(options, () -> new FirefoxDriver(options));
    }

    private static DriverPlan edge(Configuration config) {
        EdgeOptions options = new EdgeOptions();
        configure(options, config);
        if (config.isHeadless()) {
            options.addArguments("--headless=new");
        }
        return new DriverPlan(options, () -> new EdgeDriver(options));
    }

    private static void configure(AbstractDriverOptions<?> options, Configuration config) {
        options.merge(config.getCapabilities());
        options.setPageLoadStrategy(config.getPageLoadStrategy());
    }

    private static URL remoteUrl(Configuration config) {
        try {
            return new URL(config.getRemote());
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Validated remote URL is invalid: " + config.getRemote(), e);
        }
    }

    private record DriverPlan(
            org.openqa.selenium.Capabilities options,
            Supplier<WebDriver> localDriver) {
    }
}
