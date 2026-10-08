package com.listener;

import com.assertion.AssertionContext;
import com.config.ConfigLoader;
import com.config.Configuration;
import com.driver.DriverManager;
import org.testng.Assert;
import org.testng.IInvokedMethod;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestNgAssertionListenerTest {

    private static final Configuration CONFIGURATION = ConfigLoader.fromSystemProperties();
    private final TestNgAssertionListener listener = new TestNgAssertionListener();

    @BeforeMethod
    public void setUp() {
        DriverManager.setConfig(CONFIGURATION);
    }

    @AfterMethod(alwaysRun = true)
    public void cleanUp() {
        try {
            AssertionContext.finish();
        } finally {
            DriverManager.cleanup();
        }
    }

    @Test
    public void shouldFailTestWhenAutomaticAssertAllFindsSoftFailure() {
        IInvokedMethod method = testMethod();
        ITestResult result = mock(ITestResult.class);

        listener.beforeInvocation(method, result);
        AssertionContext.current().softTrue(false, "soft failure");
        listener.afterInvocation(method, result);

        verify(result).setThrowable(any(AssertionError.class));
        verify(result).setStatus(ITestResult.FAILURE);
    }

    @Test
    public void shouldKeepHardFailureAndAttachSoftFailures() {
        IInvokedMethod method = testMethod();
        AssertionError hardFailure = new AssertionError("hard failure");
        ITestResult result = mock(ITestResult.class);
        when(result.getThrowable()).thenReturn(hardFailure);
        when(result.getStatus()).thenReturn(ITestResult.FAILURE);

        listener.beforeInvocation(method, result);
        AssertionContext.current().softTrue(false, "soft failure");
        listener.afterInvocation(method, result);

        Assert.assertEquals(hardFailure.getSuppressed().length, 1);
        verify(result, never()).setThrowable(any());
        verify(result, never()).setStatus(ITestResult.FAILURE);
    }

    @Test
    public void shouldFailSuccessfulExpectedExceptionTestWhenSoftAssertionFailed() {
        IInvokedMethod method = testMethod();
        RuntimeException expectedException = new RuntimeException("expected exception");
        ITestResult result = mock(ITestResult.class);
        when(result.getStatus()).thenReturn(ITestResult.SUCCESS);
        when(result.getThrowable()).thenReturn(expectedException);

        listener.beforeInvocation(method, result);
        AssertionContext.current().softTrue(false, "soft failure");
        listener.afterInvocation(method, result);

        verify(result).setThrowable(any(AssertionError.class));
        verify(result).setStatus(ITestResult.FAILURE);
        Assert.assertEquals(expectedException.getSuppressed().length, 0);
    }

    @Test
    public void shouldFailSkippedTestWhenSoftAssertionAlreadyFailed() {
        IInvokedMethod method = testMethod();
        RuntimeException skipReason = new RuntimeException("skip reason");
        ITestResult result = mock(ITestResult.class);
        when(result.getStatus()).thenReturn(ITestResult.SKIP);
        when(result.getThrowable()).thenReturn(skipReason);

        listener.beforeInvocation(method, result);
        AssertionContext.current().softTrue(false, "soft failure");
        listener.afterInvocation(method, result);

        verify(result).setThrowable(any(AssertionError.class));
        verify(result).setStatus(ITestResult.FAILURE);
        Assert.assertEquals(skipReason.getSuppressed().length, 0);
    }

    @Test
    public void shouldIgnoreConfigurationMethods() {
        IInvokedMethod method = mock(IInvokedMethod.class);
        when(method.isTestMethod()).thenReturn(false);
        ITestResult result = mock(ITestResult.class);

        listener.beforeInvocation(method, result);
        listener.afterInvocation(method, result);

        verify(result, never()).setThrowable(any());
    }

    private static IInvokedMethod testMethod() {
        IInvokedMethod method = mock(IInvokedMethod.class);
        when(method.isTestMethod()).thenReturn(true);
        return method;
    }
}
