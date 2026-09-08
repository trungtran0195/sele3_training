package com.report.provider;

import com.config.ReportConfig;
import com.report.Reporter;
import com.report.ReporterProvider;
import com.report.TestEvent;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class ConsoleReporterProvider implements ReporterProvider {

    @Override
    public String id() {
        return "console";
    }

    @Override
    public Reporter create(ReportConfig config) {
        return new ConsoleReporter();
    }

    private static final class ConsoleReporter implements Reporter {
        private static final Logger LOGGER = Logger.getLogger(ConsoleReporter.class.getName());

        @Override
        public void onStart(TestEvent event) {
            LOGGER.info(() -> "STARTED: " + event.name());
        }

        @Override
        public void onSuccess(TestEvent event) {
            LOGGER.info(() -> "PASSED: " + event.name());
        }

        @Override
        public void onFailure(TestEvent event) {
            LOGGER.log(Level.SEVERE, "FAILED: " + event.name(), event.error());
        }

        @Override
        public void onSkipped(TestEvent event) {
            LOGGER.warning(() -> "SKIPPED: " + event.name());
        }
    }
}
