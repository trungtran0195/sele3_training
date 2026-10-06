package com.listener;

import com.assertion.AssertionContext;
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

        AssertionContext.start(DriverContext.getConfig());
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
