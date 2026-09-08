package com.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class MutableConfiguration {
    MutableDriverConfig driver = new MutableDriverConfig();
    MutableElementConfig element = new MutableElementConfig();
    MutableReportConfig report = new MutableReportConfig();

    void ensureSections() {
        driver = driver == null ? new MutableDriverConfig() : driver;
        element = element == null ? new MutableElementConfig() : element;
        report = report == null ? new MutableReportConfig() : report;
        driver.options = driver.options == null ? new HashMap<>() : driver.options;
        report.reporters = report.reporters == null
                ? new ArrayList<>(List.of("console"))
                : report.reporters;
        report.options = report.options == null ? new HashMap<>() : report.options;
    }
}

final class MutableDriverConfig {
    String browser = "chrome";
    boolean headless;
    String baseUrl = "http://localhost:8080";
    String remoteUrl = "";
    boolean startMaximized = true;
    String browserSize = "1366x768";
    Duration pageLoadTimeout = Duration.ofSeconds(30);
    String pageLoadStrategy = "normal";
    Map<String, Object> options = new HashMap<>();
}

final class MutableElementConfig {
    Duration timeout = Duration.ofSeconds(4);
    Duration pollingInterval = Duration.ofMillis(200);
}

final class MutableReportConfig {
    boolean enabled = true;
    List<String> reporters = new ArrayList<>(List.of("console"));
    String outputPath = "target/reports";
    boolean screenshotOnFailure = true;
    Map<String, Map<String, Object>> options = new HashMap<>();
}
