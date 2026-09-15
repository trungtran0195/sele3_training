package com.driver.provider;

import com.config.Configuration;
import com.config.ConfigurationException;
import com.driver.DriverProvider;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URI;

/** Shares remote-driver and capability handling between browser providers. */
public abstract class AbstractDriverProvider<O extends MutableCapabilities> implements DriverProvider {

    @Override
    public final WebDriver create(Configuration configuration) {
        O options = createOptions(configuration);
        configuration.getCapabilities().asMap().forEach(options::setCapability);
        options.setCapability("pageLoadStrategy", configuration.getPageLoadStrategy().toString());

        if (!configuration.isRemote()) {
            return createLocal(options);
        }

        try {
            return new RemoteWebDriver(URI.create(configuration.getRemote()).toURL(), options);
        } catch (MalformedURLException e) {
            throw new ConfigurationException("Invalid remote WebDriver URL", e);
        }
    }

    protected abstract O createOptions(Configuration configuration);

    protected abstract WebDriver createLocal(O options);
}
