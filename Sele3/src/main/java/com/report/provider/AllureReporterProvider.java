package com.report.provider;

import com.config.ReportConfig;
import com.report.Reporter;
import com.report.ReporterProvider;
import com.report.TestEvent;
import io.qameta.allure.Allure;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

public final class AllureReporterProvider implements ReporterProvider {

    @Override
    public String id() {
        return "allure";
    }

    @Override
    public Reporter create(ReportConfig config) {
        return new AllureReporter();
    }

    private static final class AllureReporter implements Reporter {
        @Override
        public void onStart(TestEvent event) {
            Allure.step("Started: " + event.name());
        }

        @Override
        public void onSuccess(TestEvent event) {
            Allure.step("Passed: " + event.name());
        }

        @Override
        public void onFailure(TestEvent event) {
            if (event.error() != null) {
                StringWriter trace = new StringWriter();
                event.error().printStackTrace(new PrintWriter(trace));
                Allure.addAttachment("Failure", "text/plain", trace.toString(), ".txt");
            }
        }

        @Override
        public void onSkipped(TestEvent event) {
            Allure.step("Skipped: " + event.name());
        }

        @Override
        public void attach(String name, String mediaType, byte[] content) {
            Allure.addAttachment(name, mediaType, new ByteArrayInputStream(content), "");
        }
    }
}
