package com.config;

import org.openqa.selenium.PageLoadStrategy;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class ConfigurationValueParser {

    static boolean parseBoolean(String key, String value) {
        if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
            throw new ConfigurationException(key + " must be true or false");
        }
        return Boolean.parseBoolean(value);
    }

    static Duration parseDuration(String key, String value) {
        try {
            String normalized = value.trim();
            return normalized.matches("-?\\d+")
                    ? Duration.ofMillis(Long.parseLong(normalized))
                    : Duration.parse(normalized);
        } catch (RuntimeException e) {
            throw new ConfigurationException(key + " must be milliseconds or an ISO-8601 duration", e);
        }
    }

    static PageLoadStrategy parsePageLoadStrategy(String key, String value) {
        try {
            return PageLoadStrategy.fromString(normalizeId(key, value));
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException(key + " has an unsupported value: " + value, e);
        }
    }

    static Path parsePath(String key, String value) {
        if (value == null || value.isBlank()) {
            throw new ConfigurationException(key + " must not be blank");
        }
        try {
            return Path.of(value).normalize();
        } catch (InvalidPathException e) {
            throw new ConfigurationException(key + " is not a valid path: " + value, e);
        }
    }

    static List<String> parseProviderIds(String key, String value) {
        List<String> ids = new ArrayList<>();
        for (String id : value.split(",")) {
            ids.add(normalizeId(key, id));
        }
        return List.copyOf(ids);
    }

    static String normalizeId(String key, String value) {
        if (value == null || value.isBlank()) {
            throw new ConfigurationException(key + " must not be blank");
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
