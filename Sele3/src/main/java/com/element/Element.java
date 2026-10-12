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
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Select;

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
     * Creates a lazy child element. Each operation finds the parent again and uses the first
     * matching child. Operations that require visibility apply that requirement separately.
     *
     * @param childLocator locator relative to this element
     * @return lazy child element
     */
    public Element child(By childLocator) {
        Objects.requireNonNull(childLocator, "Child locator must not be null");
        return new Element(driver -> findFirstChild(driver, childLocator));
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

    /** Waits until the element is visible, then moves the pointer over it. */
    public void hover() {
        createWait("hover over element", currentDriver -> {
            WebElement element = findVisible(currentDriver);
            if (element == null) {
                return null;
            }
            new Actions(currentDriver).moveToElement(element).perform();
            return Boolean.TRUE;
        }).await();
    }

    /** Waits until the element is visible, then scrolls it to the center of the viewport. */
    public void scrollIntoView() {
        createWait("scroll element into view", currentDriver -> {
            WebElement element = findVisible(currentDriver);
            if (element == null) {
                return null;
            }
            scrollToCenter(currentDriver, element);
            return Boolean.TRUE;
        }).await();
    }

    /** Selects an option in a native {@code <select>} element by its visible text. */
    public void selectByVisibleText(String text) {
        Objects.requireNonNull(text, "Option text must not be null");
        selectOption("select option by visible text", select -> select.selectByVisibleText(text));
    }

    /** Selects an option in a native {@code <select>} element by its value attribute. */
    public void selectByValue(String value) {
        Objects.requireNonNull(value, "Option value must not be null");
        selectOption("select option by value", select -> select.selectByValue(value));
    }

    /** Selects an option in a native {@code <select>} element by its zero-based index. */
    public void selectByIndex(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("Option index must not be negative");
        }
        selectOption("select option by index", select -> select.selectByIndex(index));
    }

    /** Selects this element if it is not already selected. */
    public void check() {
        setSelected(true);
    }

    /** Deselects this element if it is currently selected. */
    public void uncheck() {
        setSelected(false);
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

    /** Waits until the element is editable, then clears it and enters the supplied value. */
    public void setValue(String value) {
        Objects.requireNonNull(value, "Value must not be null");
        createWait("set element value", currentDriver -> {
            WebElement element = findVisible(currentDriver);
            if (element == null || !element.isEnabled() || isReadOnly(element)) {
                return null;
            }
            try {
                element.clear();
                if (!value.isEmpty()) {
                    element.sendKeys(value);
                }
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
        return createWait("read element attribute '" + validName + "'", driver -> {
            WebElement element = findElement(driver);
            return element == null ? null : new WaitResult<>(element.getAttribute(validName));
        })
                .await()
                .value();
    }

    /** Returns the element's current displayed state without waiting. */
    public boolean isDisplayed() {
        WebDriver driver = DriverContext.getDriver();
        try {
            return isCurrentlyDisplayed(driver);
        } catch (StaleElementReferenceException e) {
            // The DOM may have replaced the element between lookup and state checking.
            return isCurrentlyDisplayed(driver);
        }
    }

    /**
     * Waits until the element exists and reads its enabled state. Visibility is not required.
     *
     * @return enabled state
     */
    public boolean isEnabled() {
        return createWait("read element enabled state", driver -> {
            WebElement element = findElement(driver);
            return element == null ? null : new WaitResult<>(element.isEnabled());
        })
                .await()
                .value();
    }

    /**
     * Waits until the element exists and reads its selected state. Visibility is not required.
     *
     * @return selected state
     */
    public boolean isSelected() {
        return createWait("read element selected state", driver -> {
            WebElement element = findElement(driver);
            return element == null ? null : new WaitResult<>(element.isSelected());
        })
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
        return createWait(requireNonBlank(conditionDescription, "Condition description"), currentDriver -> {
            WebElement element = findElement(currentDriver);
            return element != null && condition.test(element) ? Boolean.TRUE : null;
        });
    }

    private WebElement findElement(WebDriver driver) {
        // Every retry runs the stored lookup again instead of reusing an old WebElement.
        return elementFinder.apply(driver);
    }

    private WebElement findVisible(WebDriver driver) {
        WebElement element = findElement(driver);
        return element != null && element.isDisplayed() ? element : null;
    }

    private boolean isCurrentlyDisplayed(WebDriver driver) {
        try {
            WebElement element = findElement(driver);
            return element != null && element.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    private boolean isReadOnly(WebElement element) {
        return Boolean.parseBoolean(element.getDomProperty("readOnly"));
    }

    private void scrollToCenter(WebDriver driver, WebElement element) {
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block: 'center', inline: 'center'});",
                element);
    }

    private WebElement findFirstChild(WebDriver driver, By childLocator) {
        WebElement parent = findElement(driver);
        if (parent == null) {
            return null;
        }
        List<WebElement> children = parent.findElements(childLocator);
        return children.isEmpty() ? null : children.get(0);
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

    private void selectOption(String actionDescription, Consumer<Select> selection) {
        createWait(actionDescription, currentDriver -> {
            WebElement element = findVisible(currentDriver);
            if (element == null || !element.isEnabled()) {
                return null;
            }
            try {
                selection.accept(new Select(element));
                return Boolean.TRUE;
            } catch (InvalidElementStateException e) {
                return null;
            }
        }).await();
    }

    private void setSelected(boolean selected) {
        String actionDescription = selected ? "check element" : "uncheck element";
        createWait(actionDescription, currentDriver -> {
            WebElement element = findVisible(currentDriver);
            if (element == null || !element.isEnabled()) {
                return null;
            }
            if (element.isSelected() == selected) {
                return Boolean.TRUE;
            }
            try {
                element.click();
            } catch (InvalidElementStateException e) {
                return null;
            }
            return element.isSelected() == selected ? Boolean.TRUE : null;
        }).await();
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
