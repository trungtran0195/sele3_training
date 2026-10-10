package com.element;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.InvalidElementStateException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.time.Duration;

import static com.element.ElementTestSupport.driverReturning;
import static com.element.ElementTestSupport.element;
import static com.element.ElementTestSupport.webDriver;
import static com.element.ElementTestSupport.webElement;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Verifies Element polling, transient-exception retry, and timeout behavior. */
public class ElementRetryTest {

    @AfterMethod(alwaysRun = true)
    public void cleanUpDriverContext() {
        ElementTestSupport.clearContext();
    }

    @Test
    public void shouldFindElementAgainAfterItBecomesStale() {
        By locator = By.id("status");
        WebElement stale = webElement();
        WebElement current = webElement();
        WebDriver driver = webDriver();
        when(driver.findElement(locator)).thenReturn(stale, current);
        when(stale.isDisplayed()).thenThrow(new StaleElementReferenceException("DOM changed"));
        when(current.isDisplayed()).thenReturn(true);
        when(current.getText()).thenReturn("Updated");

        Assert.assertEquals(element(locator, driver).getText(), "Updated");
        verify(driver, times(2)).findElement(locator);
    }

    @Test
    public void shouldRetryWhenElementDoesNotExistYet() {
        By locator = By.id("status");
        WebElement webElement = webElement();
        WebDriver driver = webDriver();
        when(driver.findElement(locator))
                .thenThrow(new NoSuchElementException("Not rendered yet"))
                .thenReturn(webElement);
        when(webElement.isDisplayed()).thenReturn(true);
        when(webElement.getText()).thenReturn("Ready");

        Assert.assertEquals(element(locator, driver).getText(), "Ready");
        verify(driver, times(2)).findElement(locator);
    }

    @Test
    public void shouldWaitUntilElementIsEnabledBeforeClicking() {
        WebElement webElement = webElement();
        when(webElement.isDisplayed()).thenReturn(true);
        when(webElement.isEnabled()).thenReturn(false, true);

        element(By.id("submit"), driverReturning(webElement)).click();

        verify(webElement, times(2)).isEnabled();
        verify(webElement).click();
    }

    @Test
    public void shouldFindElementAgainAfterScrollingAnInterceptedClick() {
        By locator = By.id("submit");
        WebElement first = webElement();
        WebElement current = webElement();
        WebDriver driver = webDriver();
        JavascriptExecutor javascript = (JavascriptExecutor) driver;
        when(driver.findElement(locator)).thenReturn(first, current);
        when(first.isDisplayed()).thenReturn(true);
        when(first.isEnabled()).thenReturn(true);
        when(current.isDisplayed()).thenReturn(true);
        when(current.isEnabled()).thenReturn(true);
        doThrow(new ElementClickInterceptedException("Outside viewport")).when(first).click();

        element(locator, driver).click();

        var order = inOrder(driver, first, current);
        order.verify(driver).findElement(locator);
        order.verify(first).click();
        order.verify(javascript).executeScript(anyString(), any());
        order.verify(driver).findElement(locator);
        order.verify(current).click();
    }

    @Test
    public void shouldRetrySendKeysForInvalidElementState() {
        WebElement webElement = webElement();
        when(webElement.isDisplayed()).thenReturn(true);
        when(webElement.isEnabled()).thenReturn(true);
        doThrow(new InvalidElementStateException("Temporarily readonly"))
                .doNothing()
                .when(webElement)
                .sendKeys(any(CharSequence[].class));

        element(By.id("input"), driverReturning(webElement)).sendKeys("value");

        verify(webElement, times(2)).sendKeys(any(CharSequence[].class));
    }

    @Test
    public void shouldWaitUntilReadonlyInputBecomesEditable() {
        WebElement webElement = webElement();
        when(webElement.isDisplayed()).thenReturn(true);
        when(webElement.isEnabled()).thenReturn(true);
        when(webElement.getDomProperty("readOnly")).thenReturn("true", "false");

        element(By.id("input"), driverReturning(webElement)).sendKeys("value");

        verify(webElement, times(2)).getDomProperty("readOnly");
        verify(webElement).sendKeys(any(CharSequence[].class));
    }

    @Test
    public void shouldRetryClearForInvalidElementState() {
        WebElement webElement = webElement();
        when(webElement.isDisplayed()).thenReturn(true);
        doThrow(new InvalidElementStateException("Not editable yet"))
                .doNothing()
                .when(webElement)
                .clear();

        element(By.id("input"), driverReturning(webElement)).clear();

        verify(webElement, times(2)).clear();
    }

    @Test
    public void shouldRetryWhenChildDoesNotExistYet() {
        By parentLocator = By.id("form");
        By childLocator = By.className("message");
        WebElement parent = webElement();
        WebElement child = webElement();
        WebDriver driver = webDriver();
        when(driver.findElement(parentLocator)).thenReturn(parent);
        when(parent.findElements(childLocator)).thenReturn(java.util.List.of(), java.util.List.of(child));
        when(child.isDisplayed()).thenReturn(true);
        when(child.getText()).thenReturn("Ready");

        Assert.assertEquals(element(parentLocator, driver).child(childLocator).getText(), "Ready");
        verify(parent, times(2)).findElements(childLocator);
    }

    @Test
    public void shouldRetryReadingChildAttributeUntilChildExists() {
        By parentLocator = By.id("form");
        By childLocator = By.id("status");
        WebElement parent = webElement();
        WebElement child = webElement();
        WebDriver driver = webDriver();
        when(driver.findElement(parentLocator)).thenReturn(parent);
        when(parent.findElements(childLocator))
                .thenReturn(java.util.List.of(), java.util.List.of(child));
        when(child.getAttribute("data-state")).thenReturn("ready");

        String state = element(parentLocator, driver)
                .child(childLocator)
                .getAttribute("data-state");

        Assert.assertEquals(state, "ready");
        verify(parent, times(2)).findElements(childLocator);
    }

    @Test
    public void shouldRetryReadingChildStateUntilChildExists() {
        By parentLocator = By.id("form");
        By childLocator = By.id("option");
        WebElement parent = webElement();
        WebElement child = webElement();
        WebDriver driver = webDriver();
        when(driver.findElement(parentLocator)).thenReturn(parent);
        when(parent.findElements(childLocator))
                .thenReturn(
                        java.util.List.of(),
                        java.util.List.of(child),
                        java.util.List.of(),
                        java.util.List.of(child));
        when(child.isEnabled()).thenReturn(true);
        when(child.isSelected()).thenReturn(true);
        Element childElement = element(parentLocator, driver).child(childLocator);

        Assert.assertTrue(childElement.isEnabled());
        Assert.assertTrue(childElement.isSelected());
        verify(parent, times(4)).findElements(childLocator);
    }

    @Test
    public void shouldRetryCustomConditionUntilChildExists() {
        By parentLocator = By.id("form");
        By childLocator = By.id("status");
        WebElement parent = webElement();
        WebElement child = webElement();
        WebDriver driver = webDriver();
        when(driver.findElement(parentLocator)).thenReturn(parent);
        when(parent.findElements(childLocator))
                .thenReturn(java.util.List.of(), java.util.List.of(child));
        when(child.isEnabled()).thenReturn(true);

        Boolean enabled = element(parentLocator, driver)
                .child(childLocator)
                .waitFor("child enabled", WebElement::isEnabled)
                .await();

        Assert.assertTrue(enabled);
        verify(parent, times(2)).findElements(childLocator);
    }

    @Test
    public void shouldRetryNestedChildUntilItsParentExists() {
        By rootLocator = By.id("form");
        By sectionLocator = By.className("section");
        By messageLocator = By.className("message");
        WebElement root = webElement();
        WebElement section = webElement();
        WebElement message = webElement();
        WebDriver driver = webDriver();
        when(driver.findElement(rootLocator)).thenReturn(root);
        when(root.findElements(sectionLocator))
                .thenReturn(java.util.List.of(), java.util.List.of(section));
        when(section.findElements(messageLocator)).thenReturn(java.util.List.of(message));
        when(message.isDisplayed()).thenReturn(true);
        when(message.getText()).thenReturn("Ready");

        String text = element(rootLocator, driver)
                .child(sectionLocator)
                .child(messageLocator)
                .getText();

        Assert.assertEquals(text, "Ready");
        verify(root, times(2)).findElements(sectionLocator);
        verify(section).findElements(messageLocator);
    }

    @Test
    public void shouldUseCustomTimeoutMessage() {
        WebElement hiddenElement = webElement();
        when(hiddenElement.isDisplayed()).thenReturn(false);
        ElementWait<Boolean> wait = element(By.id("submit"), driverReturning(hiddenElement))
                .waitFor("submit button visible", WebElement::isDisplayed)
                .withTimeout(Duration.ofMillis(20))
                .pollingEvery(Duration.ofMillis(5))
                .withMessage("Submit button did not become available");

        ElementTimeoutException error = Assert.expectThrows(ElementTimeoutException.class, wait::await);

        Assert.assertEquals(error.getMessage(), "Submit button did not become available");
        Assert.assertNull(error.getCause());
    }

    @Test
    public void shouldUseConditionDescriptionAsDefaultTimeoutMessage() {
        WebElement hiddenElement = webElement();
        when(hiddenElement.isDisplayed()).thenReturn(false);
        ElementWait<Boolean> wait = element(By.id("submit"), driverReturning(hiddenElement))
                .waitFor("find visible Submit button", WebElement::isDisplayed)
                .withTimeout(Duration.ofMillis(20))
                .pollingEvery(Duration.ofMillis(5));

        ElementTimeoutException error = Assert.expectThrows(ElementTimeoutException.class, wait::await);

        Assert.assertEquals(error.getMessage(), "find visible Submit button");
    }

    @Test
    public void shouldPropagateTimeoutThrownByTheCondition() {
        WebElement webElement = webElement();
        TimeoutException commandTimeout = new TimeoutException("Selenium command timed out");
        ElementWait<Boolean> wait = element(By.id("status"), driverReturning(webElement))
                .waitFor(
                        "status should become ready",
                        ignored -> {
                            throw commandTimeout;
                        });

        TimeoutException failure = Assert.expectThrows(TimeoutException.class, wait::await);

        Assert.assertSame(failure, commandTimeout);
        Assert.assertFalse(failure instanceof ElementTimeoutException);
    }
}
