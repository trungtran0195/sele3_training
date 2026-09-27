package com.element;

import com.config.Configuration;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.InvalidElementStateException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A lazy, locator-based element that resolves a fresh {@link WebElement} for every operation.
 * Element instances can be declared before a browser is initialized. Driver access and waiting
 * begin only when an action, read, or condition is executed.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class Element {

    @NonNull
    private final Function<WebDriver, WebElement> resolver;
    @NonNull
    private final ElementContext context;
    @NonNull
    private final String description;

    /**
     * Creates a lazy element from a Selenium locator.
     *
     * @param locator locator used to resolve the element
     * @return lazy element
     */
    public static Element of(By locator) {
        return create(locator, ElementContext.currentThread());
    }

    static Element create(
            By locator,
            Supplier<WebDriver> driverSupplier,
            Supplier<Configuration> configurationSupplier) {
        return create(locator, ElementContext.of(driverSupplier, configurationSupplier));
    }

    private static Element create(By locator, ElementContext context) {
        Objects.requireNonNull(locator, "Locator must not be null");
        // Store the lookup operation, not its WebElement result. Calling resolver.apply(driver)
        // later executes driver.findElement(locator) again and obtains the current DOM element.
        return new Element(driver -> driver.findElement(locator), context, locator.toString());
    }

    /**
     * Creates a lazy child element. The parent and child are resolved again for every operation,
     * so no parent {@link WebElement} is cached.
     *
     * @param childLocator locator relative to this element
     * @return lazy child element
     */
    public Element child(By childLocator) {
        Objects.requireNonNull(childLocator, "Child locator must not be null");
        // Re-running this resolver first resolves a fresh parent, then finds a fresh child from it.
        return new Element(
                driver -> resolver.apply(driver).findElement(childLocator),
                context,
                description + " -> " + childLocator);
    }

    /**
     * Returns an immutable copy with a readable name used in timeout messages.
     *
     * @param elementName readable element name
     * @return named element
     */
    public Element named(String elementName) {
        return new Element(resolver, context, requireNonBlank(elementName, "Element name"));
    }

    /** Waits until the element is visible and enabled, then clicks it. */
    public void click() {
        waitUntil("click " + description, currentDriver -> {
            WebElement element = findVisible(currentDriver);
            if (element == null || !element.isEnabled()) {
                return null;
            }
            element.click();
            return Boolean.TRUE;
        }).ignoring(ElementClickInterceptedException.class, ElementNotInteractableException.class)
                .await();
    }

    /**
     * Waits until the element is visible, then sends the supplied keys.
     *
     * @param keys character sequences to send
     */
    public void sendKeys(CharSequence... keys) {
        Objects.requireNonNull(keys, "Keys must not be null");
        performWhenVisible("send keys to " + description, element -> element.sendKeys(keys))
                .ignoring(ElementNotInteractableException.class)
                .await();
    }

    /** Waits until the element is visible and editable, then clears it. */
    public void clear() {
        performWhenVisible("clear " + description, WebElement::clear)
                .ignoring(InvalidElementStateException.class)
                .await();
    }

    /**
     * Waits until the element is visible and returns its rendered text.
     *
     * @return visible element text
     */
    public String getText() {
        return readWhenVisible("read text from " + description, WebElement::getText);
    }

    /**
     * Waits until the element exists and reads an attribute. Visibility is not required.
     *
     * @param attributeName attribute to read
     * @return attribute value, including {@code null} when the attribute is absent
     */
    public String getAttribute(String attributeName) {
        String validName = requireNonBlank(attributeName, "Attribute name");
        return read(
                "read attribute '" + validName + "' from " + description,
                element -> element.getAttribute(validName));
    }

    /**
     * Waits for visibility and returns whether the element becomes displayed.
     *
     * @return {@code true} when visible before timeout; otherwise {@code false}
     */
    public boolean isDisplayed() {
        try {
            resolveVisible();
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * Waits until the element exists and reads its enabled state. Visibility is not required.
     *
     * @return enabled state
     */
    public boolean isEnabled() {
        return read("read enabled state of " + description, WebElement::isEnabled);
    }

    /**
     * Waits until the element exists and reads its selected state. Visibility is not required.
     *
     * @return selected state
     */
    public boolean isSelected() {
        return read("read selected state of " + description, WebElement::isSelected);
    }

    /**
     * Creates a configurable wait for a custom action after the element exists.
     *
     * @param actionDescription description used in timeout messages
     * @param action custom Selenium action
     * @return configurable wait; the action runs only when {@link ElementWait#await()} is called
     */
    public ElementWait<Boolean> perform(
            String actionDescription,
            Consumer<WebElement> action) {
        Objects.requireNonNull(action, "Element action must not be null");
        return waitUntil(requireNonBlank(actionDescription, "Action description"), currentDriver -> {
            action.accept(resolve(currentDriver));
            return Boolean.TRUE;
        });
    }

    /**
     * Executes a custom read after the element exists. A successful read may return {@code null}.
     *
     * @param readDescription description used in timeout messages
     * @param reader custom element reader
     * @param <T> result type
     * @return read result, including {@code null}
     */
    public <T> T read(
            String readDescription,
            Function<WebElement, T> reader) {
        Objects.requireNonNull(reader, "Element reader must not be null");
        return waitUntil(requireNonBlank(readDescription, "Read description"), currentDriver ->
                new WaitResult<>(reader.apply(resolve(currentDriver)))).await().value();
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
        return waitUntil(requireNonBlank(conditionDescription, "Condition description"), currentDriver ->
                condition.test(resolve(currentDriver)) ? Boolean.TRUE : null);
    }

    private WebElement resolveVisible() {
        return waitUntil("find visible " + description, this::findVisible).await();
    }

    private WebElement resolve(WebDriver driver) {
        // The resolver is a saved locator lambda. Every invocation performs findElement again;
        // therefore a wait retry never reuses the WebElement from the previous attempt.
        return resolver.apply(driver);
    }

    private WebElement findVisible(WebDriver driver) {
        WebElement element = resolve(driver);
        return element.isDisplayed() ? element : null;
    }

    private ElementWait<Boolean> performWhenVisible(
            String actionDescription,
            Consumer<WebElement> action) {
        return waitUntil(actionDescription, currentDriver -> {
            WebElement element = findVisible(currentDriver);
            if (element == null) {
                return null;
            }
            action.accept(element);
            return Boolean.TRUE;
        });
    }

    private <T> T readWhenVisible(
            String readDescription,
            Function<WebElement, T> reader) {
        return waitUntil(readDescription, currentDriver -> {
            WebElement element = findVisible(currentDriver);
            return element == null ? null : new WaitResult<>(reader.apply(element));
        }).await().value();
    }

    private <T> ElementWait<T> waitUntil(
            String defaultTimeoutMessage,
            Function<WebDriver, T> condition) {
        Configuration configuration = context.configuration();
        return ElementWait.forCondition(condition)
                .usingDriver(context.driver())
                .withTimeout(configuration.getTimeout())
                .pollingEvery(configuration.getPollingInterval())
                .withMessage(defaultTimeoutMessage);
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    @Override
    public String toString() {
        return "Element{" + description + '}';
    }

    private record WaitResult<T>(T value) {
    }
}
