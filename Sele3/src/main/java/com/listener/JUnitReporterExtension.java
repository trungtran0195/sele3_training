package com.listener;

import com.report.TestEvent;
import com.report.TestStatus;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import java.util.Map;

public final class JUnitReporterExtension
        implements BeforeTestExecutionCallback, TestWatcher, AfterAllCallback {

    private final ReporterListener listener = new ReporterListener();

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        listener.onStart(event(context, TestStatus.STARTED, null));
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        listener.onSuccess(event(context, TestStatus.PASSED, null));
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        listener.onFailure(event(context, TestStatus.FAILED, cause));
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        listener.onSkipped(event(context, TestStatus.SKIPPED, cause));
    }

    @Override
    public void testDisabled(ExtensionContext context, java.util.Optional<String> reason) {
        listener.onStart(event(context, TestStatus.STARTED, null));
        listener.onSkipped(new TestEvent(
                context.getUniqueId(),
                context.getDisplayName(),
                TestStatus.SKIPPED,
                null,
                Map.of("reason", reason.orElse("disabled"))));
    }

    @Override
    public void afterAll(ExtensionContext context) {
        listener.flush();
    }

    private TestEvent event(ExtensionContext context, TestStatus status, Throwable error) {
        return new TestEvent(
                context.getUniqueId(),
                context.getRequiredTestClass().getName() + "." + context.getRequiredTestMethod().getName(),
                status,
                error,
                Map.of("displayName", context.getDisplayName(), "tags", context.getTags()));
    }
}
