package com.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.driver.browser.Browser;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public class ConfigLoader {

    private static final Pattern BROWSER_SIZE = Pattern.compile("[1-9]\\d*x[1-9]\\d*");
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Duration.class, new DurationTypeAdapter())
            .registerTypeAdapter(Browser.class, new BrowserTypeAdapter())
            .create();

    private ConfigLoader() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static Configuration fromJsonFile(String jsonFile) {
        Configuration config = load(jsonFile);
        applySystemPropertyOverrides(config);
        validate(config);
        return config;
    }

    public static Configuration fromSystemProperties() {
        Configuration config = new Configuration();
        applySystemPropertyOverrides(config);
        validate(config);
        return config;
    }

    private static Configuration load(String file) {
        Objects.requireNonNull(file, "Configuration file must not be null");
        Path path = Path.of(file);
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Configuration config = GSON.fromJson(reader, Configuration.class);
            if (config == null) {
                throw new IllegalArgumentException("Configuration file is empty: " + path);
            }
            return config;
        } catch (IOException | JsonParseException e) {
            throw new RuntimeException("Cannot load config: " + file, e);
        }
    }

    private static void applySystemPropertyOverrides(Configuration config) {
        config.setBrowser(browserProperty(ConfigKey.BROWSER, config.getBrowser()));
        config.setHeadless(booleanProperty(ConfigKey.HEADLESS, config.isHeadless()));
        config.setRemote(textProperty(ConfigKey.REMOTE, config.getRemote()));
        config.setBaseUrl(textProperty(ConfigKey.BASE_URL, config.getBaseUrl()));
        config.setBrowserSize(textProperty(ConfigKey.BROWSER_SIZE, config.getBrowserSize()));
        config.setStartMaximized(booleanProperty(ConfigKey.START_MAXIMIZED, config.isStartMaximized()));
        config.setTimeout(durationProperty(ConfigKey.TIMEOUT, config.getTimeout()));
        config.setPageLoadTimeout(durationProperty(ConfigKey.PAGE_LOAD_TIMEOUT, config.getPageLoadTimeout()));
        config.setPollingInterval(durationProperty(ConfigKey.POLLING_INTERVAL, config.getPollingInterval()));

        String strategy = System.getProperty(ConfigKey.PAGE_LOAD_STRATEGY);
        if (strategy != null) {
            try {
                config.setPageLoadStrategy(PageLoadStrategy.fromString(strategy.trim().toLowerCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                throw invalidProperty(ConfigKey.PAGE_LOAD_STRATEGY, strategy, e);
            }
        }
    }

    private static String textProperty(String key, String fallback) {
        return System.getProperty(key, fallback);
    }

    private static boolean booleanProperty(String key, boolean fallback) {
        String value = System.getProperty(key);
        if (value == null) {
            return fallback;
        }
        if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
            throw invalidProperty(key, value, null);
        }
        return Boolean.parseBoolean(value);
    }

    private static Browser browserProperty(String key, Browser fallback) {
        String value = System.getProperty(key);
        return value == null ? fallback : Browser.from(value);
    }

    private static Duration durationProperty(String key, Duration fallback) {
        String value = System.getProperty(key);
        if (value == null) {
            return fallback;
        }
        try {
            return Duration.ofMillis(Long.parseLong(value));
        } catch (NumberFormatException e) {
            throw invalidProperty(key, value, e);
        }
    }

    private static IllegalArgumentException invalidProperty(String key, String value, Exception cause) {
        return new IllegalArgumentException("Invalid system property '" + key + "': " + value, cause);
    }

    private static void validate(Configuration config) {
        if (config.getBrowser() == null) {
            throw new IllegalArgumentException(ConfigKey.BROWSER + " must not be null");
        }
        requireText(ConfigKey.BASE_URL, config.getBaseUrl());
        requireHttpUrl(ConfigKey.BASE_URL, config.getBaseUrl());
        if (config.isRemote()) {
            requireHttpUrl(ConfigKey.REMOTE, config.getRemote());
        }
        requireText(ConfigKey.BROWSER_SIZE, config.getBrowserSize());
        if (!BROWSER_SIZE.matcher(config.getBrowserSize()).matches()) {
            throw new IllegalArgumentException("browserSize must use WIDTHxHEIGHT format: " + config.getBrowserSize());
        }
        requirePositive(ConfigKey.TIMEOUT, config.getTimeout());
        requirePositive(ConfigKey.PAGE_LOAD_TIMEOUT, config.getPageLoadTimeout());
        requirePositive(ConfigKey.POLLING_INTERVAL, config.getPollingInterval());
        if (config.getPollingInterval().compareTo(config.getTimeout()) > 0) {
            throw new IllegalArgumentException("pollingInterval must not exceed timeout");
        }
        if (config.getPageLoadStrategy() == null) {
            config.setPageLoadStrategy(PageLoadStrategy.NORMAL);
        }
        if (config.getCapabilities() == null) {
            config.setCapabilities(new DesiredCapabilities());
        }
    }

    private static void requireText(String key, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(key + " must not be blank");
        }
    }

    private static void requirePositive(String key, Duration value) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(key + " must be greater than zero");
        }
    }

    private static void requireHttpUrl(String key, String value) {
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            if (uri.getHost() == null || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                throw new IllegalArgumentException(key + " must be an absolute HTTP(S) URL: " + value);
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(key + " is not a valid URL: " + value, e);
        }
    }
}
