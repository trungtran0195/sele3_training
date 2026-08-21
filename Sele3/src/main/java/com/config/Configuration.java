package com.config;

import lombok.Getter;
import lombok.Setter;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.time.Duration;

@Getter
@Setter
public class Configuration {
    private String browser;
    private boolean headless;
    private String baseUrl;
    private String remote;
    private boolean startMaximized;
    private String browserSize;
    private Duration timeout;
    private Duration pageLoadTimeout;
    private Duration pollingInterval;
    private MutableCapabilities capabilities;

    public Configuration(boolean initial) {
        if (initial) {
            this.browser = System.getProperty(ConfigKey.BROWSER, "chrome");
            this.headless = Boolean.parseBoolean(System.getProperty(ConfigKey.HEADLESS, "false"));
            this.baseUrl = System.getProperty(ConfigKey.BASE_URL, "http://localhost:8080");
            this.remote = System.getProperty(ConfigKey.REMOTE, "");
            this.startMaximized = Boolean.parseBoolean(System.getProperty(ConfigKey.START_MAXIMIZED, "true"));
            this.browserSize = System.getProperty(ConfigKey.BROWSER_SIZE, "1366x768");
            this.timeout = Duration.ofMillis(Long.parseLong(System.getProperty(ConfigKey.TIMEOUT, "4000")));
            this.pageLoadTimeout = Duration.ofMillis(Long.parseLong(System.getProperty(ConfigKey.PAGE_LOAD_TIMEOUT, "30000")));
            this.pollingInterval = Duration.ofMillis(Long.parseLong(System.getProperty(ConfigKey.POLLING_INTERVAL, "200")));
            this.capabilities = new DesiredCapabilities();
        }
    }

    public boolean isRemote() {
        return remote != null && !remote.isBlank();
    }
}