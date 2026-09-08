package com.report;

import com.config.ReportConfig;
import com.extension.NamedProvider;

public interface ReporterProvider extends NamedProvider {
    Reporter create(ReportConfig config);
}
