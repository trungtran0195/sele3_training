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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class TestNgAssertionListenerTest {

    private static final Configuration CONFIGURATION = ConfigLoader.fromJsonFile(Path.of(
            "src", "test", "resources", "config", "defaults.json").toString());

    private final TestNgAssertionListener listener = new TestNgAssertionListener();

    @BeforeMethod
    public void setUp() {
        DriverManager.setConfig(CONFIGURATION);
    }

    @AfterMethod(alwaysRun = true)
    public void cleanUp() {
        DriverManager.cleanup();
    }

    @Test
    public void shouldFailTestWhenAutomaticAssertAllFindsSoftFailure() {
        IInvokedMethod method = testMethod();
        TestResultState state = new TestResultState();
        ITestResult result = testResult(state);

        listener.beforeInvocation(method, result);
        AssertionContext.current().softTrue(false, "soft failure");
        listener.afterInvocation(method, result);

        Assert.assertEquals(state.status.get(), ITestResult.FAILURE);
        Assert.assertTrue(state.throwable.get() instanceof AssertionError);
        Assert.expectThrows(IllegalStateException.class, AssertionContext::current);
    }

    @Test
    public void shouldKeepHardFailureAndAttachSoftFailures() {
        IInvokedMethod method = testMethod();
        AssertionError hardFailure = new AssertionError("hard failure");
        TestResultState state = new TestResultState();
        state.throwable.set(hardFailure);
        ITestResult result = testResult(state);

        listener.beforeInvocation(method, result);
        AssertionContext.current().softTrue(false, "soft failure");
        listener.afterInvocation(method, result);

        Assert.assertSame(state.throwable.get(), hardFailure);
        Assert.assertEquals(hardFailure.getSuppressed().length, 1);
    }

    @Test
    public void shouldIgnoreConfigurationMethods() {
        TestResultState state = new TestResultState();

        listener.beforeInvocation(configurationMethod(), testResult(state));

        Assert.expectThrows(IllegalStateException.class, AssertionContext::current);
    }

    private static IInvokedMethod testMethod() {
        return invokedMethod(true);
    }

    private static IInvokedMethod configurationMethod() {
        return invokedMethod(false);
    }

    private static IInvokedMethod invokedMethod(boolean testMethod) {
        return proxy(IInvokedMethod.class, (proxy, method, args) ->
                method.getName().equals("isTestMethod") ? testMethod : defaultValue(method));
    }

    private static ITestResult testResult(TestResultState state) {
        return proxy(ITestResult.class, (proxy, method, args) -> switch (method.getName()) {
            case "getThrowable" -> state.throwable.get();
            case "setThrowable" -> {
                state.throwable.set((Throwable) args[0]);
                yield null;
            }
            case "setStatus" -> {
                state.status.set((Integer) args[0]);
                yield null;
            }
            default -> defaultValue(method);
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private static Object defaultValue(Method method) {
        Class<?> type = method.getReturnType();
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        return 0;
    }

    private static final class TestResultState {
        private final AtomicReference<Throwable> throwable = new AtomicReference<>();
        private final AtomicInteger status = new AtomicInteger(ITestResult.SUCCESS);
    }
}
