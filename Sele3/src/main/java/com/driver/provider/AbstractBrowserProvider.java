package com.driver.provider;

import com.config.DriverConfig;
import com.driver.DriverProvider;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.ImmutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.AbstractDriverOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Supplier;

abstract class AbstractBrowserProvider implements DriverProvider {

    protected void applyCommonOptions(AbstractDriverOptions<?> options, DriverConfig config) {
        options.merge(new ImmutableCapabilities(config.getOptions()));
        options.setPageLoadStrategy(config.getPageLoadStrategy());
    }

    protected WebDriver create(
            DriverConfig config,
            Capabilities capabilities,
            Supplier<WebDriver> localCreator) {
        return config.isRemote()
                ? new RemoteWebDriver(remoteUrl(config), capabilities)
                : localCreator.get();
    }

    private URL remoteUrl(DriverConfig config) {
        try {
            return new URL(config.getRemoteUrl());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid remote WebDriver URL: " + config.getRemoteUrl(), e);
        }
    }
}
