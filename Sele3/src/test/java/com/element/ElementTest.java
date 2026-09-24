package com.element;

import com.config.ConfigLoader;
import com.config.Configuration;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

public class ElementTest {

    private static final Configuration CONFIGURATION = ConfigLoader.fromJsonFile(Path.of(
            "src", "test", "resources", "config", "defaults.json").toString());
    private static final Configuration SHORT_TIMEOUT_CONFIGURATION = ConfigLoader.fromJsonFile(Path.of(
            "src", "test", "resources", "config", "short-timeout.json").toString());

    @Test
    public void shouldUseXpathWhenLocatorTypeIsNull() {
        Element element = Element.of(null, "//button");

        Assert.assertEquals(element.toString(), "Element{By.xpath: //button}");
    }

    @Test
    public void shouldResolveAVisibleElementForEachRead() {
        AtomicInteger finds = new AtomicInteger();
        WebElement webElement = webElement((proxy, method, args) -> switch (method.getName()) {
            case "isDisplayed" -> true;
            case "getText" -> "Welcome";
            default -> defaultValue(method);
        });
        WebDriver driver = webDriver((proxy, method, args) -> {
            if (method.getName().equals("findElement")) {
                finds.incrementAndGet();
                return webElement;
            }
            return defaultValue(method);
        });
        Element element = Element.create(By.id("message"), () -> driver, () -> CONFIGURATION);

        Assert.assertEquals(element.getText(), "Welcome");
        Assert.assertEquals(element.getText(), "Welcome");
        Assert.assertEquals(finds.get(), 2);
    }

    @Test
    public void shouldRefindElementAfterItBecomesStale() {
        AtomicInteger finds = new AtomicInteger();
        WebElement stale = webElement((proxy, method, args) -> {
            if (method.getName().equals("isDisplayed")) {
                throw new StaleElementReferenceException("DOM changed");
            }
            return defaultValue(method);
        });
        WebElement current = webElement((proxy, method, args) -> switch (method.getName()) {
            case "isDisplayed" -> true;
            case "getText" -> "Updated";
            default -> defaultValue(method);
        });
        WebDriver driver = webDriver((proxy, method, args) -> {
            if (method.getName().equals("findElement")) {
                return finds.getAndIncrement() == 0 ? stale : current;
            }
            return defaultValue(method);
        });
        Element element = Element.create(By.id("status"), () -> driver, () -> CONFIGURATION);

        Assert.assertEquals(element.getText(), "Updated");
        Assert.assertEquals(finds.get(), 2);
    }

    @Test
    public void shouldWaitUntilElementIsClickable() {
        AtomicInteger enabledChecks = new AtomicInteger();
        AtomicInteger clicks = new AtomicInteger();
        WebElement webElement = webElement((proxy, method, args) -> switch (method.getName()) {
            case "isDisplayed" -> true;
            case "isEnabled" -> enabledChecks.incrementAndGet() > 1;
            case "click" -> {
                clicks.incrementAndGet();
                yield null;
            }
            default -> defaultValue(method);
        });
        WebDriver driver = driverReturning(webElement);
        Element element = Element.create(By.id("submit"), () -> driver, () -> CONFIGURATION);

        element.click();

        Assert.assertEquals(clicks.get(), 1);
        Assert.assertTrue(enabledChecks.get() >= 2);
    }

    @Test
    public void shouldResolveNestedElementWithoutStoringParentWebElement() {
        AtomicInteger parentFinds = new AtomicInteger();
        WebElement child = webElement((proxy, method, args) -> switch (method.getName()) {
            case "isDisplayed" -> true;
            case "getText" -> "Nested";
            default -> defaultValue(method);
        });
        WebElement parent = webElement((proxy, method, args) -> {
            if (method.getName().equals("findElement")) {
                return child;
            }
            return defaultValue(method);
        });
        WebDriver driver = webDriver((proxy, method, args) -> {
            if (method.getName().equals("findElement")) {
                parentFinds.incrementAndGet();
                return parent;
            }
            return defaultValue(method);
        });
        Element form = Element.create(By.id("form"), () -> driver, () -> CONFIGURATION);

        Element message = form.child(LocatorType.CLASS_NAME, "message");
        Assert.assertEquals(message.getText(), "Nested");
        Assert.assertEquals(parentFinds.get(), 1);
    }

    @Test
    public void shouldCreateChildElementWithoutAccessingDriver() {
        Element parent = Element.create(
                By.id("form"),
                () -> {
                    throw new AssertionError("Driver must not be accessed while defining an element");
                },
                () -> {
                    throw new AssertionError("Configuration must not be accessed while defining an element");
                });

        Element child = parent.child(By.id("message"));

        Assert.assertEquals(
                child.toString(),
                "Element{By.id: form -> By.id: message}");
    }

    @Test
    public void shouldKeepInteractionRetryPolicyScopedToClick() {
        AtomicInteger clicks = new AtomicInteger();
        AtomicInteger textReads = new AtomicInteger();
        WebElement webElement = webElement((proxy, method, args) -> switch (method.getName()) {
            case "isDisplayed", "isEnabled" -> true;
            case "click" -> {
                if (clicks.incrementAndGet() == 1) {
                    throw new ElementClickInterceptedException("Overlay is still visible");
                }
                yield null;
            }
            case "getText" -> {
                textReads.incrementAndGet();
                throw new ElementClickInterceptedException("Unexpected read failure");
            }
            default -> defaultValue(method);
        });
        Element element = Element.create(
                By.id("submit"),
                () -> driverReturning(webElement),
                () -> SHORT_TIMEOUT_CONFIGURATION);

        element.click();
        Assert.expectThrows(ElementClickInterceptedException.class, element::getText);

        Assert.assertEquals(clicks.get(), 2);
        Assert.assertEquals(textReads.get(), 1);
    }

    @Test
    public void shouldReturnNullWhenVisibleElementHasNoAttribute() {
        WebElement webElement = webElement((proxy, method, args) ->
                method.getName().equals("isDisplayed") ? true : defaultValue(method));
        Element element = Element.create(
                By.id("optional"),
                () -> driverReturning(webElement),
                () -> CONFIGURATION);

        Assert.assertNull(element.getAttribute("data-value"));
    }

    @Test
    public void shouldUseCustomTimeoutMessage() {
        WebElement hiddenElement = webElement((proxy, method, args) ->
                method.getName().equals("isDisplayed") ? false : defaultValue(method));
        Element element = Element.create(
                        By.id("submit"),
                        () -> driverReturning(hiddenElement),
                        () -> SHORT_TIMEOUT_CONFIGURATION)
                .withTimeoutMessage("Submit button did not become available");

        ElementTimeoutException error = Assert.expectThrows(
                ElementTimeoutException.class,
                element::click);

        Assert.assertEquals(error.getMessage(), "Submit button did not become available");
        Assert.assertNotNull(error.getCause());
    }

    @Test
    public void shouldIncludeOperationAndElementNameInDefaultTimeoutMessage() {
        WebElement hiddenElement = webElement((proxy, method, args) ->
                method.getName().equals("isDisplayed") ? false : defaultValue(method));
        Element element = Element.create(
                        By.id("submit"),
                        () -> driverReturning(hiddenElement),
                        () -> SHORT_TIMEOUT_CONFIGURATION)
                .named("Submit button");

        ElementTimeoutException error = Assert.expectThrows(
                ElementTimeoutException.class,
                element::click);

        Assert.assertEquals(error.getMessage(), "click Submit button");
    }

    private static WebDriver driverReturning(WebElement element) {
        return webDriver((proxy, method, args) ->
                method.getName().equals("findElement") ? element : defaultValue(method));
    }

    private static WebDriver webDriver(InvocationHandler handler) {
        return proxy(WebDriver.class, handler);
    }

    private static WebElement webElement(InvocationHandler handler) {
        return proxy(WebElement.class, handler);
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
}
