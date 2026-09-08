package com.report;

import com.config.ReportConfig;
import com.extension.ProviderRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ReporterRegistry {

    private final ProviderRegistry<ReporterProvider> registry;

    public ReporterRegistry() {
        this(Thread.currentThread().getContextClassLoader());
    }

    public ReporterRegistry(ClassLoader classLoader) {
        registry = new ProviderRegistry<>(ReporterProvider.class, classLoader);
    }

    public List<Reporter> create(ReportConfig config) {
        if (!config.isEnabled()) {
            return List.of();
        }

        List<Reporter> reporters = new ArrayList<>();
        for (String configuredId : config.getReporters()) {
            reporters.add(registry.require(configuredId).create(config));
        }
        return List.copyOf(reporters);
    }

    public Map<String, ReporterProvider> providers() {
        return registry.providers();
    }
}
