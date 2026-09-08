package com.config;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Value
@Builder(access = AccessLevel.PACKAGE)
public class ReportConfig {
    boolean enabled;
    List<String> reporters;
    Path outputPath;
    boolean screenshotOnFailure;
    Map<String, Map<String, Object>> options;

    public Map<String, Object> optionsFor(String reporterId) {
        return options.getOrDefault(reporterId, Map.of());
    }
}
