package com.listener;

import com.assertion.AssertionContext;
import com.config.Configuration;
import com.driver.DriverContext;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;

/** Connects the runner-independent assertion lifecycle to TestNG test methods. */
public final class TestNgAssertionListener implements IInvokedMethodListener {

    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
        if (!method.isTestMethod()) {
            return;
        }

        Configuration configuration = DriverContext.getConfig();
        if (configuration == null) {
            throw new IllegalStateException("Configuration must be initialized before assertion session starts");
        }
        AssertionContext.start(configuration);
    }

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        if (!method.isTestMethod()) {
            return;
        }

        try {
            AssertionContext.finish();
        } catch (AssertionError softFailure) {
            Throwable testFailure = testResult.getThrowable();
            if (testFailure != null) {
                testFailure.addSuppressed(softFailure);
                return;
            }
            testResult.setThrowable(softFailure);
            testResult.setStatus(ITestResult.FAILURE);
        }
    }
}
