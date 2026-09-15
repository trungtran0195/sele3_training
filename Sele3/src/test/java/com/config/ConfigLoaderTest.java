package com.config;

import org.openqa.selenium.PageLoadStrategy;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.time.Duration;

public class ConfigLoaderTest {

    private static final String CONFIG_PATH = Path.of(
            "src", "test", "resources", "config", "chrome.json").toString();
    private static final String DEFAULTS_CONFIG_PATH = Path.of(
            "src", "test", "resources", "config", "defaults.json").toString();

    @AfterMethod
    public void clearOverrides() {
        System.clearProperty(ConfigKey.BROWSER);
        System.clearProperty(ConfigKey.HEADLESS);
        System.clearProperty(ConfigKey.TIMEOUT);
    }

    @Test
    public void shouldLoadJsonConfiguration() {
        Configuration configuration = ConfigLoader.fromJsonFile(CONFIG_PATH);

        Assert.assertEquals(configuration.getBrowser(), "chrome");
        Assert.assertEquals(configuration.getTimeout(), Duration.ofSeconds(5));
        Assert.assertEquals(configuration.getPageLoadStrategy(), PageLoadStrategy.NORMAL);
    }

    @Test
    public void shouldApplySystemPropertyOverrides() {
        System.setProperty(ConfigKey.BROWSER, "firefox");
        System.setProperty(ConfigKey.HEADLESS, "true");
        System.setProperty(ConfigKey.TIMEOUT, "7000");

        Configuration configuration = ConfigLoader.fromJsonFile(CONFIG_PATH);

        Assert.assertEquals(configuration.getBrowser(), "firefox");
        Assert.assertTrue(configuration.isHeadless());
        Assert.assertEquals(configuration.getTimeout(), Duration.ofSeconds(7));
    }

    @Test
    public void shouldUseDefaultsWhenJsonFieldsAreMissing() {
        Configuration configuration = ConfigLoader.fromJsonFile(DEFAULTS_CONFIG_PATH);

        Assert.assertEquals(configuration.getBrowser(), "chrome");
        Assert.assertEquals(configuration.getBrowserSize(), "1366x768");
        Assert.assertEquals(configuration.getTimeout(), Duration.ofSeconds(4));
        Assert.assertEquals(configuration.getPollingInterval(), Duration.ofMillis(200));
    }

    @Test(expectedExceptions = ConfigurationException.class)
    public void shouldFailWhenConfigurationFileDoesNotExist() {
        ConfigLoader.fromJsonFile("missing-config.json");
    }
}
