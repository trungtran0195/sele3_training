package com.element;

import com.config.Configuration;
import com.driver.DriverContext;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.InvalidElementStateException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A lazy, locator-based element that finds a fresh {@link WebElement} for every operation.
 * Element instances can be declared before a browser is initialized. Driver access and waiting
 * begin only when an action, read, or condition is executed.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class Element {

    @NonNull
    private final Function<WebDriver, WebElement> elementFinder;

    /**
     * Creates a lazy element from a Selenium locator.
     *
     * @param locator locator used to resolve the element
     * @return lazy element
     */
    public static Element of(By locator) {
        Objects.requireNonNull(locator, "Locator must not be null");
        // Store how to find the element, not a WebElement that may later become stale.
        return new Element(driver -> driver.findElement(locator));
    }

    /**
     * Creates a lazy child element. When an operation is executed, Selenium's
     * {@link ExpectedConditions#visibilityOfNestedElementsLocatedBy(WebElement, By)} finds the
     * visible children inside a freshly found parent. The first matching child is used.
     *
     * @param childLocator locator relative to this element
     * @return lazy child element
     */
    public Element child(By childLocator) {
        Objects.requireNonNull(childLocator, "Child locator must not be null");
        return new Element(driver -> findFirstVisibleChild(driver, childLocator));
    }

    /** Waits until the element is visible and enabled, then clicks it. */
    public void click() {
        createWait("click element", currentDriver -> {
            WebElement element = findVisible(currentDriver);
            if (element == null || !element.isEnabled()) {
                return null;
            }
            try {
                element.click();
            } catch (ElementNotInteractableException e) {
                scrollToCenter(currentDriver, element);
                return null;
            }
            return Boolean.TRUE;
        }).await();
    }

    /**
     * Waits until the element is visible, then sends the supplied keys.
     *
     * @param keys character sequences to send
     */
    public void sendKeys(CharSequence... keys) {
        Objects.requireNonNull(keys, "Keys must not be null");
        createWait("element did not become editable", driver -> {
            WebElement element = findVisible(driver);
            if (element == null || !element.isEnabled() || isReadOnly(element)) {
                return null;
            }
            try {
                element.sendKeys(keys);
                return Boolean.TRUE;
            } catch (InvalidElementStateException e) {
                return null;
            }
        }).await();
    }

    /** Waits until the element is visible and editable, then clears it. */
    public void clear() {
        performWhenVisible("clear element", WebElement::clear)
                .ignoring(InvalidElementStateException.class)
                .await();
    }

    /**
     * Waits until the element is visible and returns its rendered text.
     *
     * @return visible element text
     */
    public String getText() {
        return createWait("read element text", driver -> {
            WebElement element = findVisible(driver);
            return element == null ? null : new WaitResult<>(element.getText());
        }).await().value();
    }

    /**
     * Waits until the element exists and reads an attribute. Visibility is not required.
     *
     * @param attributeName attribute to read
     * @return attribute value, including {@code null} when the attribute is absent
     */
    public String getAttribute(String attributeName) {
        String validName = requireNonBlank(attributeName, "Attribute name");
        return createWait(
                "read element attribute '" + validName + "'",
                driver -> new WaitResult<>(findElement(driver).getAttribute(validName)))
                .await()
                .value();
    }

    /** Returns the element's current displayed state without waiting. */
    public boolean isDisplayed() {
        try {
            WebElement element = findElement(DriverContext.getDriver());
            return element != null && element.isDisplayed();
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            return false;
        }
    }

    /**
     * Waits until the element exists and reads its enabled state. Visibility is not required.
     *
     * @return enabled state
     */
    public boolean isEnabled() {
        return createWait(
                "read element enabled state",
                driver -> new WaitResult<>(findElement(driver).isEnabled()))
                .await()
                .value();
    }

    /**
     * Waits until the element exists and reads its selected state. Visibility is not required.
     *
     * @return selected state
     */
    public boolean isSelected() {
        return createWait(
                "read element selected state",
                driver -> new WaitResult<>(findElement(driver).isSelected()))
                .await()
                .value();
    }

    /**
     * Creates a configurable wait for a custom element condition. Timeout, message, polling, and
     * ignored exceptions belong only to the returned condition.
     *
     * @param conditionDescription description used as the default timeout message
     * @param condition condition evaluated against a freshly resolved element
     * @return wait for this condition
     */
    public ElementWait<Boolean> waitFor(
            String conditionDescription,
            Predicate<WebElement> condition) {
        Objects.requireNonNull(condition, "Element condition must not be null");
        return createWait(requireNonBlank(conditionDescription, "Condition description"), currentDriver ->
                condition.test(findElement(currentDriver)) ? Boolean.TRUE : null);
    }

    private WebElement findElement(WebDriver driver) {
        // Every retry runs the stored lookup again instead of reusing an old WebElement.
        return elementFinder.apply(driver);
    }

    private WebElement findVisible(WebDriver driver) {
        WebElement element = findElement(driver);
        return element != null && element.isDisplayed() ? element : null;
    }

    private boolean isReadOnly(WebElement element) {
        return Boolean.parseBoolean(element.getDomProperty("readOnly"));
    }

    private void scrollToCenter(WebDriver driver, WebElement element) {
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block: 'center', inline: 'center'});",
                element);
    }

    private WebElement findFirstVisibleChild(WebDriver driver, By childLocator) {
        WebElement parent = findElement(driver);
        List<WebElement> visibleChildren = ExpectedConditions
                .visibilityOfNestedElementsLocatedBy(parent, childLocator)
                .apply(driver);
        return visibleChildren == null || visibleChildren.isEmpty() ? null : visibleChildren.get(0);
    }

    private ElementWait<Boolean> performWhenVisible(
            String actionDescription,
            Consumer<WebElement> action) {
        return createWait(actionDescription, currentDriver -> {
            WebElement element = findVisible(currentDriver);
            if (element == null) {
                return null;
            }
            action.accept(element);
            return Boolean.TRUE;
        });
    }

    private <T> ElementWait<T> createWait(
            String message,
            Function<WebDriver, T> condition) {
        WebDriver driver = DriverContext.getDriver();

        Configuration configuration = DriverContext.getConfig();
        if (configuration == null) {
            throw new IllegalStateException("Configuration has not been initialized for this thread");
        }

        return ElementWait.forCondition(condition)
                .usingDriver(driver)
                .withTimeout(configuration.getTimeout())
                .pollingEvery(configuration.getPollingInterval())
                .withMessage(message);
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private record WaitResult<T>(T value) {
    }
}
