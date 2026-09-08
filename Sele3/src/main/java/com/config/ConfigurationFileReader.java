package com.config;

import com.config.adapter.DurationTypeAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.Properties;

final class ConfigurationFileReader {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Duration.class, new DurationTypeAdapter())
            .create();

    static MutableConfiguration read(String file) {
        ConfigurationResolver.requireNonBlank("Configuration file", file);
        String normalized = file.toLowerCase(Locale.ROOT);
        if (normalized.endsWith(".json")) {
            return fromJson(file);
        }
        if (normalized.endsWith(".properties")) {
            return fromProperties(file);
        }
        throw new ConfigurationException(
                "Configuration file must end with .json or .properties: " + file);
    }

    private static MutableConfiguration fromJson(String file) {
        Path path = configurationFile(file);
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            MutableConfiguration config = GSON.fromJson(reader, MutableConfiguration.class);
            if (config == null) {
                throw new ConfigurationException("Configuration file is empty: " + path);
            }
            config.ensureSections();
            return config;
        } catch (IOException | JsonParseException e) {
            throw new ConfigurationException("Cannot load JSON configuration: " + file, e);
        }
    }

    private static MutableConfiguration fromProperties(String file) {
        Path path = configurationFile(file);
        Properties properties = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException e) {
            throw new ConfigurationException("Cannot load properties configuration: " + file, e);
        }

        MutableConfiguration config = new MutableConfiguration();
        ConfigurationOverrides.applyProperties(config, properties);
        return config;
    }

    private static Path configurationFile(String file) {
        ConfigurationResolver.requireNonBlank("Configuration file", file);
        return ConfigurationValueParser.parsePath("Configuration file", file);
    }
}
