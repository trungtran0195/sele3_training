package com.config;

import com.driver.browser.Browser;
import lombok.Getter;
import lombok.Setter;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.time.Duration;

@Getter
@Setter
public class Configuration {
    private Browser browser = Browser.CHROME;
    private boolean headless = false;
    private String baseUrl = "http://localhost:8080";
    private String remote = "";
    private boolean startMaximized = true;
    private String browserSize = "1366x768";
    private Duration timeout = Duration.ofSeconds(4);
    private Duration pageLoadTimeout = Duration.ofSeconds(30);
    private Duration pollingInterval = Duration.ofMillis(200);
    private PageLoadStrategy pageLoadStrategy = PageLoadStrategy.NORMAL;
    private MutableCapabilities capabilities = new DesiredCapabilities();

    public Configuration() {
        // Defaults are initialized at field declaration so Gson preserves them
        // when optional properties are absent from the JSON file.
    }

    public Configuration(Configuration source) {
        this.browser = source.browser;
        this.headless = source.headless;
        this.baseUrl = source.baseUrl;
        this.remote = source.remote;
        this.startMaximized = source.startMaximized;
        this.browserSize = source.browserSize;
        this.timeout = source.timeout;
        this.pageLoadTimeout = source.pageLoadTimeout;
        this.pollingInterval = source.pollingInterval;
        this.pageLoadStrategy = source.pageLoadStrategy;
        this.capabilities = source.capabilities == null
                ? new DesiredCapabilities()
                : new DesiredCapabilities(source.capabilities);
    }

    public boolean isRemote() {
        return remote != null && !remote.isBlank();
    }
}
