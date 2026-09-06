package com.config;

import com.config.adapter.DurationTypeAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import org.openqa.selenium.ImmutableCapabilities;
import org.openqa.selenium.PageLoadStrategy;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class ConfigLoader {

    private static final Pattern BROWSER_SIZE = Pattern.compile("[1-9]\\d*x[1-9]\\d*");
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Duration.class, new DurationTypeAdapter())
            .create();

    public static Configuration fromJsonFile(String jsonFile) {
        MutableConfiguration config = load(jsonFile);
        applySystemPropertyOverrides(config);
        return createValidatedConfiguration(config);
    }

    public static Configuration fromSystemProperties() {
        MutableConfiguration config = new MutableConfiguration();
        applySystemPropertyOverrides(config);
        return createValidatedConfiguration(config);
    }

    private static MutableConfiguration load(String file) {
        if (file == null || file.isBlank()) {
            throw new ConfigurationException("Configuration file must not be blank");
        }

        Path path = Path.of(file);
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            MutableConfiguration config = GSON.fromJson(reader, MutableConfiguration.class);
            if (config == null) {
                throw new ConfigurationException("Configuration file is empty: " + path);
            }
            return config;
        } catch (IOException | JsonParseException e) {
            throw new ConfigurationException("Cannot load config: " + file, e);
        }
    }

    private static void applySystemPropertyOverrides(MutableConfiguration config) {
        config.browser = browserProperty(ConfigKey.BROWSER, config.browser);
        config.headless = booleanProperty(ConfigKey.HEADLESS, config.headless);
        config.remote = textProperty(ConfigKey.REMOTE, config.remote);
        config.baseUrl = textProperty(ConfigKey.BASE_URL, config.baseUrl);
        config.browserSize = textProperty(ConfigKey.BROWSER_SIZE, config.browserSize);
        config.startMaximized = booleanProperty(ConfigKey.START_MAXIMIZED, config.startMaximized);
        config.timeout = durationProperty(ConfigKey.TIMEOUT, config.timeout);
        config.pageLoadTimeout = durationProperty(ConfigKey.PAGE_LOAD_TIMEOUT, config.pageLoadTimeout);
        config.pollingInterval = durationProperty(ConfigKey.POLLING_INTERVAL, config.pollingInterval);
        config.pageLoadStrategy = textProperty(ConfigKey.PAGE_LOAD_STRATEGY, config.pageLoadStrategy);
    }

    private static Configuration createValidatedConfiguration(MutableConfiguration config) {
        if (config.browser == null) {
            throw new ConfigurationException(ConfigKey.BROWSER + " must not be null");
        }
        requireHttpUrl(ConfigKey.BASE_URL, config.baseUrl);
        if (config.remote != null && !config.remote.isBlank()) {
            requireHttpUrl(ConfigKey.REMOTE, config.remote);
        }
        requireNonBlank(ConfigKey.BROWSER_SIZE, config.browserSize);
        if (!BROWSER_SIZE.matcher(config.browserSize).matches()) {
            throw new ConfigurationException("browserSize must use WIDTHxHEIGHT format: " + config.browserSize);
        }
        requirePositive(ConfigKey.TIMEOUT, config.timeout);
        requirePositive(ConfigKey.PAGE_LOAD_TIMEOUT, config.pageLoadTimeout);
        requirePositive(ConfigKey.POLLING_INTERVAL, config.pollingInterval);
        if (config.pollingInterval.compareTo(config.timeout) > 0) {
            throw new ConfigurationException("pollingInterval must not exceed timeout");
        }

        PageLoadStrategy pageLoadStrategy = pageLoadStrategy(config.pageLoadStrategy);
        Map<String, Object> capabilities = config.capabilities == null
                ? Map.of()
                : Map.copyOf(config.capabilities);

        return Configuration.builder()
                .browser(config.browser)
                .headless(config.headless)
                .baseUrl(config.baseUrl)
                .remote(config.remote == null ? "" : config.remote)
                .startMaximized(config.startMaximized)
                .browserSize(config.browserSize)
                .timeout(config.timeout)
                .pageLoadTimeout(config.pageLoadTimeout)
                .pollingInterval(config.pollingInterval)
                .pageLoadStrategy(pageLoadStrategy)
                .capabilities(new ImmutableCapabilities(capabilities))
                .build();
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
        if (value == null) {
            return fallback;
        }
        try {
            return Browser.from(value);
        } catch (ConfigurationException e) {
            throw invalidProperty(key, value, e);
        }
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

    private static PageLoadStrategy pageLoadStrategy(String value) {
        requireNonBlank(ConfigKey.PAGE_LOAD_STRATEGY, value);
        try {
            return PageLoadStrategy.fromString(value.trim().toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException("Invalid pageLoadStrategy: " + value, e);
        }
    }

    private static ConfigurationException invalidProperty(String key, String value, Throwable cause) {
        return new ConfigurationException("Invalid system property '" + key + "': " + value, cause);
    }

    private static void requireNonBlank(String key, String value) {
        if (value == null || value.isBlank()) {
            throw new ConfigurationException(key + " must not be blank");
        }
    }

    private static void requirePositive(String key, Duration value) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new ConfigurationException(key + " must be greater than zero");
        }
    }

    private static void requireHttpUrl(String key, String value) {
        requireNonBlank(key, value);
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            if (uri.getHost() == null || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                throw new ConfigurationException(key + " must be an absolute HTTP(S) URL: " + value);
            }
        } catch (URISyntaxException e) {
            throw new ConfigurationException(key + " is not a valid URL: " + value, e);
        }
    }

    private static final class MutableConfiguration {
        private Browser browser = Browser.CHROME;
        private boolean headless;
        private String baseUrl = "http://localhost:8080";
        private String remote = "";
        private boolean startMaximized = true;
        private String browserSize = "1366x768";
        private Duration timeout = Duration.ofSeconds(4);
        private Duration pageLoadTimeout = Duration.ofSeconds(30);
        private Duration pollingInterval = Duration.ofMillis(200);
        private String pageLoadStrategy = "normal";
        private Map<String, Object> capabilities = new HashMap<>();
    }
}
