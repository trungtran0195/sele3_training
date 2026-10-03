package com.element;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.InvalidElementStateException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.element.ElementTestSupport.defaultValue;
import static com.element.ElementTestSupport.driverReturning;
import static com.element.ElementTestSupport.element;
import static com.element.ElementTestSupport.webDriver;
import static com.element.ElementTestSupport.webElement;

/** Verifies Element polling, transient-exception retry, and timeout behavior. */
public class ElementRetryTest {

    @AfterMethod(alwaysRun = true)
    public void cleanUpDriverManager() {
        ElementTestSupport.clearContext();
    }

    @Test
    public void shouldFindElementAgainAfterItBecomesStale() {
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
        WebDriver driver = webDriver((proxy, method, args) ->
                method.getName().equals("findElement")
                        ? (finds.getAndIncrement() == 0 ? stale : current)
                        : defaultValue(method));

        Assert.assertEquals(element(By.id("status"), driver).getText(), "Updated");
        Assert.assertEquals(finds.get(), 2);
    }

    @Test
    public void shouldRetryWhenElementDoesNotExistYet() {
        AtomicInteger finds = new AtomicInteger();
        WebElement webElement = webElement((proxy, method, args) -> switch (method.getName()) {
            case "isDisplayed" -> true;
            case "getText" -> "Ready";
            default -> defaultValue(method);
        });
        WebDriver driver = webDriver((proxy, method, args) -> {
            if (method.getName().equals("findElement")) {
                if (finds.incrementAndGet() == 1) {
                    throw new NoSuchElementException("Not rendered yet");
                }
                return webElement;
            }
            return defaultValue(method);
        });

        Assert.assertEquals(element(By.id("status"), driver).getText(), "Ready");
        Assert.assertEquals(finds.get(), 2);
    }

    @Test
    public void shouldWaitUntilElementIsEnabledBeforeClicking() {
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

        element(By.id("submit"), driverReturning(webElement)).click();

        Assert.assertEquals(clicks.get(), 1);
        Assert.assertTrue(enabledChecks.get() >= 2);
    }

    @Test
    public void shouldRetryWholeClickWhenClickAfterScrollStillFails() {
        List<String> events = new ArrayList<>();
        AtomicInteger clicks = new AtomicInteger();
        WebElement webElement = webElement((proxy, method, args) -> switch (method.getName()) {
            case "isDisplayed", "isEnabled" -> true;
            case "click" -> {
                events.add("click");
                int attempt = clicks.incrementAndGet();
                if (attempt == 1) {
                    throw new ElementClickInterceptedException("Element is outside the viewport");
                }
                if (attempt == 2) {
                    throw new ElementNotInteractableException("Element is still not interactable");
                }
                yield null;
            }
            default -> defaultValue(method);
        });
        WebDriver driver = webDriver((proxy, method, args) -> switch (method.getName()) {
            case "findElement" -> webElement;
            case "executeScript" -> {
                events.add("scroll");
                yield null;
            }
            default -> defaultValue(method);
        });

        element(By.id("submit"), driver).click();

        Assert.assertEquals(events, List.of("click", "scroll", "click", "click"));
        Assert.assertEquals(clicks.get(), 3);
    }

    @Test
    public void shouldRetrySendKeysForInvalidElementState() {
        AtomicInteger sends = new AtomicInteger();
        WebElement webElement = webElement((proxy, method, args) -> {
            if (method.getName().equals("isDisplayed") || method.getName().equals("isEnabled")) {
                return true;
            }
            if (method.getName().equals("sendKeys") && sends.incrementAndGet() == 1) {
                throw new InvalidElementStateException("Input is temporarily readonly");
            }
            return defaultValue(method);
        });

        element(By.id("input"), driverReturning(webElement)).sendKeys("value");

        Assert.assertEquals(sends.get(), 2);
    }

    @Test
    public void shouldWaitUntilReadonlyInputBecomesEditable() {
        AtomicInteger readonlyChecks = new AtomicInteger();
        AtomicInteger sends = new AtomicInteger();
        WebElement webElement = webElement((proxy, method, args) -> switch (method.getName()) {
            case "isDisplayed", "isEnabled" -> true;
            case "getDomProperty" -> readonlyChecks.incrementAndGet() == 1 ? "true" : "false";
            case "sendKeys" -> {
                sends.incrementAndGet();
                yield null;
            }
            default -> defaultValue(method);
        });

        element(By.id("input"), driverReturning(webElement)).sendKeys("value");

        Assert.assertEquals(readonlyChecks.get(), 2);
        Assert.assertEquals(sends.get(), 1);
    }

    @Test
    public void shouldRetryClearForInvalidElementState() {
        AtomicInteger clears = new AtomicInteger();
        WebElement webElement = webElement((proxy, method, args) -> {
            if (method.getName().equals("isDisplayed")) {
                return true;
            }
            if (method.getName().equals("clear") && clears.incrementAndGet() == 1) {
                throw new InvalidElementStateException("Input is not editable yet");
            }
            return defaultValue(method);
        });

        element(By.id("input"), driverReturning(webElement)).clear();

        Assert.assertEquals(clears.get(), 2);
    }

    @Test
    public void shouldUseCustomTimeoutMessage() {
        WebElement hiddenElement = webElement((proxy, method, args) ->
                method.getName().equals("isDisplayed") ? false : defaultValue(method));
        ElementWait<Boolean> wait = element(By.id("submit"), driverReturning(hiddenElement))
                .waitFor("submit button visible", WebElement::isDisplayed)
                .withTimeout(Duration.ofMillis(20))
                .pollingEvery(Duration.ofMillis(5))
                .withMessage("Submit button did not become available");

        ElementTimeoutException error = Assert.expectThrows(
                ElementTimeoutException.class,
                wait::await);

        Assert.assertEquals(error.getMessage(), "Submit button did not become available");
        Assert.assertNotNull(error.getCause());
    }

    @Test
    public void shouldUseConditionDescriptionAsDefaultTimeoutMessage() {
        WebElement hiddenElement = webElement((proxy, method, args) ->
                method.getName().equals("isDisplayed") ? false : defaultValue(method));
        ElementWait<Boolean> wait = element(By.id("submit"), driverReturning(hiddenElement))
                .waitFor("find visible Submit button", WebElement::isDisplayed)
                .withTimeout(Duration.ofMillis(20))
                .pollingEvery(Duration.ofMillis(5));

        ElementTimeoutException error = Assert.expectThrows(
                ElementTimeoutException.class,
                wait::await);

        Assert.assertEquals(error.getMessage(), "find visible Submit button");
    }
}
