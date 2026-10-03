package com.element;

import com.config.ConfigLoader;
import com.config.Configuration;
import com.driver.DriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

final class ElementTestSupport {

    private static final Configuration CONFIGURATION = ConfigLoader.fromSystemProperties();

    private ElementTestSupport() {
    }

    static Element element(By locator, WebDriver driver) {
        DriverManager.cleanup();
        DriverManager.setConfig(CONFIGURATION);
        driverThreadLocal().set(driver);
        return Element.of(locator);
    }

    static void clearContext() {
        DriverManager.cleanup();
    }

    static WebDriver driverReturning(WebElement element) {
        return webDriver((proxy, method, args) ->
                method.getName().equals("findElement") ? element : defaultValue(method));
    }

    static WebDriver webDriver(InvocationHandler handler) {
        return (WebDriver) Proxy.newProxyInstance(
                WebDriver.class.getClassLoader(),
                new Class<?>[]{WebDriver.class, JavascriptExecutor.class},
                handler);
    }

    static WebElement webElement(InvocationHandler handler) {
        return proxy(WebElement.class, handler);
    }

    static Object defaultValue(Method method) {
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

    @SuppressWarnings("unchecked")
    private static ThreadLocal<WebDriver> driverThreadLocal() {
        try {
            Field field = DriverManager.class.getDeclaredField("DRIVER");
            field.setAccessible(true);
            return (ThreadLocal<WebDriver>) field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Cannot install test WebDriver in DriverManager", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
}
