package com.driver;

import com.config.DriverConfig;
import com.extension.ProviderRegistry;
import org.openqa.selenium.WebDriver;

import java.util.Map;

public final class DriverRegistry {

    private final ProviderRegistry<DriverProvider> registry;

    public DriverRegistry() {
        this(Thread.currentThread().getContextClassLoader());
    }

    public DriverRegistry(ClassLoader classLoader) {
        registry = new ProviderRegistry<>(DriverProvider.class, classLoader);
    }

    public WebDriver create(DriverConfig config) {
        return registry.require(config.getBrowser()).create(config);
    }

    public Map<String, DriverProvider> providers() {
        return registry.providers();
    }
}
