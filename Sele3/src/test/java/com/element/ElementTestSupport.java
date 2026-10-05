package com.element;

import com.config.ConfigLoader;
import com.config.Configuration;
import com.driver.DriverContext;
import org.mockito.MockedStatic;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

final class ElementTestSupport {

    private static final Configuration CONFIGURATION = ConfigLoader.fromSystemProperties();
    private static final ThreadLocal<MockedStatic<DriverContext>> DRIVER_CONTEXT = new ThreadLocal<>();

    private ElementTestSupport() {
    }

    static Element element(By locator, WebDriver driver) {
        clearContext();
        MockedStatic<DriverContext> context = mockStatic(DriverContext.class);
        context.when(DriverContext::getDriver).thenReturn(driver);
        context.when(DriverContext::getConfig).thenReturn(CONFIGURATION);
        DRIVER_CONTEXT.set(context);
        return Element.of(locator);
    }

    static void clearContext() {
        MockedStatic<DriverContext> context = DRIVER_CONTEXT.get();
        if (context != null) {
            context.close();
            DRIVER_CONTEXT.remove();
        }
    }

    static WebDriver driverReturning(WebElement element) {
        WebDriver driver = webDriver();
        when(driver.findElement(any(By.class))).thenReturn(element);
        return driver;
    }

    static WebDriver webDriver() {
        return mock(WebDriver.class, withSettings().extraInterfaces(JavascriptExecutor.class));
    }

    static WebElement webElement() {
        return mock(WebElement.class);
    }
}
