package com.listener;

import com.report.TestEvent;
import com.report.TestStatus;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.util.Arrays;
import java.util.Map;

public final class TestNgReporterListener implements ITestListener {

    private final ReporterListener listener = new ReporterListener();

    @Override
    public void onTestStart(ITestResult result) {
        listener.onStart(event(result, TestStatus.STARTED));
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        listener.onSuccess(event(result, TestStatus.PASSED));
    }

    @Override
    public void onTestFailure(ITestResult result) {
        listener.onFailure(event(result, TestStatus.FAILED));
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        listener.onSkipped(event(result, TestStatus.SKIPPED));
    }

    @Override
    public void onFinish(ITestContext context) {
        listener.flush();
    }

    private TestEvent event(ITestResult result, TestStatus status) {
        String qualifiedName = result.getTestClass().getName() + "." + result.getMethod().getMethodName();
        String id = result.getTestContext().getName() + ":" + qualifiedName + ":" + result.getStartMillis();
        return new TestEvent(
                id,
                qualifiedName,
                status,
                result.getThrowable(),
                Map.of("parameters", Arrays.asList(result.getParameters())));
    }
}
