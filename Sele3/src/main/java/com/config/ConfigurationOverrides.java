package com.config;

import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Function;

final class ConfigurationOverrides {

    private static final Map<String, BiConsumer<MutableDriverConfig, String>> DRIVER_SETTERS = Map.of(
            "driver.browser", (driver, value) ->
                    driver.browser = ConfigurationValueParser.normalizeId("driver.browser", value),
            "driver.headless", (driver, value) ->
                    driver.headless = ConfigurationValueParser.parseBoolean("driver.headless", value),
            "driver.remoteUrl", (driver, value) -> driver.remoteUrl = value,
            "driver.baseUrl", (driver, value) -> driver.baseUrl = value,
            "driver.startMaximized", (driver, value) ->
                    driver.startMaximized = ConfigurationValueParser.parseBoolean(
                            "driver.startMaximized", value),
            "driver.browserSize", (driver, value) -> driver.browserSize = value,
            "driver.pageLoadTimeout", (driver, value) ->
                    driver.pageLoadTimeout = ConfigurationValueParser.parseDuration(
                            "driver.pageLoadTimeout", value),
            "driver.pageLoadStrategy", (driver, value) -> driver.pageLoadStrategy = value);

    private static final Map<String, BiConsumer<MutableElementConfig, String>> ELEMENT_SETTERS = Map.of(
            "element.timeout", (element, value) ->
                    element.timeout = ConfigurationValueParser.parseDuration("element.timeout", value),
            "element.pollingInterval", (element, value) ->
                    element.pollingInterval = ConfigurationValueParser.parseDuration(
                            "element.pollingInterval", value));

    private static final Map<String, BiConsumer<MutableReportConfig, String>> REPORT_SETTERS = Map.of(
            "report.enabled", (report, value) ->
                    report.enabled = ConfigurationValueParser.parseBoolean("report.enabled", value),
            "report.reporters", (report, value) ->
                    report.reporters = ConfigurationValueParser.parseProviderIds(
                            "report.reporters", value),
            "report.outputPath", (report, value) -> report.outputPath = value,
            "report.screenshotOnFailure", (report, value) ->
                    report.screenshotOnFailure = ConfigurationValueParser.parseBoolean(
                            "report.screenshotOnFailure", value));

    static void applyProperties(MutableConfiguration config, Properties properties) {
        apply(config, properties::getProperty);
        applyDriverOptions(config.driver, properties);
        applyReportOptions(config.report, properties);
    }

    static void applyEnvironment(MutableConfiguration config) {
        apply(config, key -> System.getenv(toEnvironmentKey(key)));
    }

    static void applySystemProperties(MutableConfiguration config) {
        apply(config, System::getProperty);
        applyDriverOptions(config.driver, System.getProperties());
        applyReportOptions(config.report, System.getProperties());
    }

    private static void apply(MutableConfiguration config, Function<String, String> source) {
        applySection(config.driver, DRIVER_SETTERS, source);
        applySection(config.element, ELEMENT_SETTERS, source);
        applySection(config.report, REPORT_SETTERS, source);
    }

    private static <T> void applySection(
            T section,
            Map<String, BiConsumer<T, String>> setters,
            Function<String, String> source) {
        setters.forEach((key, setter) -> {
            String value = source.apply(key);
            if (value != null) {
                applyValue(section, key, value, setter);
            }
        });
    }

    private static <T> void applyValue(
            T section,
            String key,
            String value,
            BiConsumer<T, String> setter) {
        try {
            setter.accept(section, value);
        } catch (ConfigurationException e) {
            throw new ConfigurationException("Invalid value for '" + key + "': " + value, e);
        }
    }

    private static void applyDriverOptions(MutableDriverConfig driver, Properties properties) {
        properties.stringPropertyNames().stream()
                .filter(key -> key.startsWith("driver.options."))
                .forEach(key -> driver.options.put(
                        key.substring("driver.options.".length()),
                        scalarValue(properties.getProperty(key))));
    }

    private static void applyReportOptions(MutableReportConfig report, Properties properties) {
        properties.stringPropertyNames().stream()
                .filter(key -> key.startsWith("report.options."))
                .forEach(key -> putNestedOption(
                        report.options,
                        key.substring("report.options.".length()),
                        scalarValue(properties.getProperty(key))));
    }

    private static void putNestedOption(
            Map<String, Map<String, Object>> options,
            String path,
            Object value) {
        int separator = path.indexOf('.');
        if (separator < 1 || separator == path.length() - 1) {
            throw new ConfigurationException(
                    "Report option must use report.options.PROVIDER.NAME format: " + path);
        }
        String provider = path.substring(0, separator).toLowerCase(Locale.ROOT);
        String name = path.substring(separator + 1);
        options.computeIfAbsent(provider, ignored -> new java.util.HashMap<>()).put(name, value);
    }

    private static Object scalarValue(String value) {
        String normalized = value.trim();
        if (normalized.equalsIgnoreCase("true") || normalized.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(normalized);
        }
        try {
            return Long.parseLong(normalized);
        } catch (NumberFormatException ignored) {
            return value;
        }
    }

    private static String toEnvironmentKey(String key) {
        return key.replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .replace('.', '_')
                .toUpperCase(Locale.ROOT);
    }
}
