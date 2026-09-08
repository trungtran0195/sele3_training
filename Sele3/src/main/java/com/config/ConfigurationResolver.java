package com.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

final class ConfigurationResolver {

    private static final Pattern BROWSER_SIZE = Pattern.compile("[1-9]\\d*x[1-9]\\d*");

    static Configuration resolve(MutableConfiguration config) {
        validate(config);

        DriverConfig driver = DriverConfig.builder()
                .browser(config.driver.browser)
                .headless(config.driver.headless)
                .baseUrl(config.driver.baseUrl)
                .remoteUrl(config.driver.remoteUrl == null ? "" : config.driver.remoteUrl)
                .startMaximized(config.driver.startMaximized)
                .browserSize(config.driver.browserSize)
                .pageLoadTimeout(config.driver.pageLoadTimeout)
                .pageLoadStrategy(ConfigurationValueParser.parsePageLoadStrategy(
                        "driver.pageLoadStrategy",
                        config.driver.pageLoadStrategy))
                .options(Map.copyOf(config.driver.options))
                .build();

        ElementConfig element = ElementConfig.builder()
                .timeout(config.element.timeout)
                .pollingInterval(config.element.pollingInterval)
                .build();

        ReportConfig report = ReportConfig.builder()
                .enabled(config.report.enabled)
                .reporters(List.copyOf(config.report.reporters))
                .outputPath(ConfigurationValueParser.parsePath(
                        "report.outputPath",
                        config.report.outputPath))
                .screenshotOnFailure(config.report.screenshotOnFailure)
                .options(copyReportOptions(config.report.options))
                .build();

        return Configuration.builder().driver(driver).element(element).report(report).build();
    }

    private static void validate(MutableConfiguration config) {
        boolean customWindowSize = config.driver.headless || !config.driver.startMaximized;
        if (customWindowSize && (config.driver.browserSize == null
                || !BROWSER_SIZE.matcher(config.driver.browserSize).matches())) {
            throw new ConfigurationException(
                    "driver.browserSize must use WIDTHxHEIGHT format: "
                            + config.driver.browserSize);
        }
        requirePositive("driver.pageLoadTimeout", config.driver.pageLoadTimeout);
        requirePositive("element.timeout", config.element.timeout);
        requirePositive("element.pollingInterval", config.element.pollingInterval);
        if (config.element.pollingInterval.compareTo(config.element.timeout) > 0) {
            throw new ConfigurationException(
                    "element.pollingInterval must not exceed element.timeout");
        }
    }

    static void requireNonBlank(String key, String value) {
        if (value == null || value.isBlank()) {
            throw new ConfigurationException(key + " must not be blank");
        }
    }

    private static void requirePositive(String key, Duration value) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new ConfigurationException(key + " must be greater than zero");
        }
    }

    private static Map<String, Map<String, Object>> copyReportOptions(
            Map<String, Map<String, Object>> options) {
        Map<String, Map<String, Object>> copy = new HashMap<>();
        options.forEach((provider, values) -> copy.put(provider, Map.copyOf(values)));
        return Map.copyOf(copy);
    }
}
