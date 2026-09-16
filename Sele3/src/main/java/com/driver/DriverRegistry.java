package com.driver;

import com.config.Configuration;
import com.config.ConfigurationException;
import org.openqa.selenium.WebDriver;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;

/** Discovers WebDriver providers available on the runtime classpath. */
public final class DriverRegistry {

    private static final Map<String, DriverProvider> PROVIDERS = loadProviders();

    public static WebDriver create(Configuration configuration) {
        String browser = normalize(configuration.getBrowser());
        DriverProvider provider = PROVIDERS.get(browser);
        if (provider == null) {
            throw new ConfigurationException(
                    "No driver provider registered for browser: " + configuration.getBrowser());
        }
        return provider.create(configuration);
    }

    static Map<String, DriverProvider> providers() {
        return PROVIDERS;
    }

    private static Map<String, DriverProvider> loadProviders() {
        Map<String, DriverProvider> providers = new LinkedHashMap<>();
        for (DriverProvider provider : ServiceLoader.load(DriverProvider.class)) {
            String name = normalize(provider.name());
            DriverProvider duplicate = providers.putIfAbsent(name, provider);
            if (duplicate != null) {
                throw new ConfigurationException("Duplicate driver provider: " + name);
            }
        }
        return Map.copyOf(providers);
    }

    private static String normalize(String name) {
        if (name == null || name.isBlank()) {
            throw new ConfigurationException("Driver provider name must not be blank");
        }
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
