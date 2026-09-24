package com.element;

import com.config.Configuration;
import com.driver.DriverContext;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Locator-based element that resolves a fresh WebElement for every operation.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class Element {

    @NonNull
    private final Function<WebDriver, WebElement> resolver;
    @NonNull
    private final Supplier<WebDriver> driverSupplier;
    @NonNull
    private final Supplier<Configuration> configurationSupplier;
    @NonNull
    private final String description;
    private final String timeoutMessageOverride;
    private final Duration timeoutOverride;

    public static Element of(By locator) {
        return create(locator, DriverContext::getDriver, DriverContext::getConfig);
    }

    public static Element of(LocatorType locatorType, String locatorValue) {
        return of(toBy(locatorType, locatorValue));
    }

    static Element create(
            By locator,
            Supplier<WebDriver> driverSupplier,
            Supplier<Configuration> configurationSupplier) {
        Objects.requireNonNull(locator, "Locator must not be null");
        return new Element(
                driver -> driver.findElement(locator),
                driverSupplier,
                configurationSupplier,
                locator.toString(),
                null,
                null);
    }

    public Element child(By childLocator) {
        Objects.requireNonNull(childLocator, "Child locator must not be null");
        return new Element(
                driver -> resolver.apply(driver).findElement(childLocator),
                driverSupplier,
                configurationSupplier,
                description + " -> " + childLocator,
                null,
                null);
    }

    public Element child(LocatorType locatorType, String locatorValue) {
        return child(toBy(locatorType, locatorValue));
    }

    private static By toBy(LocatorType locatorType, String locatorValue) {
        return (locatorType == null ? LocatorType.XPATH : locatorType).toBy(locatorValue);
    }

    public Element named(String elementName) {
        if (elementName == null || elementName.isBlank()) {
            throw new IllegalArgumentException("Element name must not be blank");
        }
        return new Element(
                resolver,
                driverSupplier,
                configurationSupplier,
                elementName,
                timeoutMessageOverride,
                timeoutOverride);
    }

    public Element withTimeoutMessage(String timeoutMessage) {
        if (timeoutMessage == null || timeoutMessage.isBlank()) {
            throw new IllegalArgumentException("Timeout message must not be blank");
        }
        return new Element(
                resolver,
                driverSupplier,
                configurationSupplier,
                description,
                timeoutMessage,
                timeoutOverride);
    }

    public Element withTimeout(Duration timeout) {
        Objects.requireNonNull(timeout, "Timeout must not be null");
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("Timeout must be greater than zero");
        }
        return new Element(
                resolver,
                driverSupplier,
                configurationSupplier,
                description,
                timeoutMessageOverride,
                timeout);
    }

    public void click() {
        waitUntil("click " + description, currentDriver -> {
            WebElement element = findVisible(currentDriver);
            if (element == null || !element.isEnabled()) {
                return null;
            }
            element.click();
            return Boolean.TRUE;
        }, ElementClickInterceptedException.class, ElementNotInteractableException.class);
    }

    public void sendKeys(CharSequence... keys) {
        Objects.requireNonNull(keys, "Keys must not be null");
        performWhenVisible(
                "send keys to " + description,
                element -> element.sendKeys(keys),
                ElementNotInteractableException.class);
    }

    public void clear() {
        performWhenVisible(
                "clear " + description,
                WebElement::clear,
                ElementNotInteractableException.class);
    }

    public String getText() {
        return readWhenVisible("read text from " + description, WebElement::getText);
    }

    public String getAttribute(String attributeName) {
        if (attributeName == null || attributeName.isBlank()) {
            throw new IllegalArgumentException("Attribute name must not be blank");
        }
        return readWhenVisible(
                "read attribute '" + attributeName + "' from " + description,
                element -> element.getAttribute(attributeName));
    }

    public boolean isDisplayed() {
        try {
            resolveVisible();
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    public boolean isEnabled() {
        return readWhenVisible("read enabled state of " + description, WebElement::isEnabled);
    }

    public boolean isSelected() {
        return readWhenVisible("read selected state of " + description, WebElement::isSelected);
    }

    private WebElement resolveVisible() {
        return waitUntil("find visible " + description, this::findVisible);
    }

    private WebElement findVisible(WebDriver driver) {
        WebElement element = resolver.apply(driver);
        return element.isDisplayed() ? element : null;
    }

    @SafeVarargs
    private final void performWhenVisible(
            String operationDescription,
            Consumer<WebElement> action,
            Class<? extends Throwable>... ignoredExceptions) {
        waitUntil(operationDescription, currentDriver -> {
            WebElement element = findVisible(currentDriver);
            if (element == null) {
                return null;
            }
            action.accept(element);
            return Boolean.TRUE;
        }, ignoredExceptions);
    }

    private <T> T readWhenVisible(String operationDescription, Function<WebElement, T> reader) {
        return waitUntil(operationDescription, currentDriver -> {
            WebElement element = findVisible(currentDriver);
            return element == null ? null : new WaitResult<>(reader.apply(element));
        }).value();
    }

    @SafeVarargs
    private final <T> T waitUntil(
            String defaultTimeoutMessage,
            Function<WebDriver, T> condition,
            Class<? extends Throwable>... ignoredExceptions) {
        WebDriver driver = driverSupplier.get();
        if (driver == null) {
            throw new IllegalStateException("WebDriver has not been initialized for this thread");
        }

        Configuration configuration = configurationSupplier.get();
        if (configuration == null) {
            throw new IllegalStateException("Configuration has not been initialized for this thread");
        }

        return ElementWait.forCondition(condition)
                .usingDriver(driver)
                .withTimeout(timeoutOverride == null ? configuration.getTimeout() : timeoutOverride)
                .pollingEvery(configuration.getPollingInterval())
                .withMessage(timeoutMessageOverride == null ? defaultTimeoutMessage : timeoutMessageOverride)
                .ignoring(ignoredExceptions)
                .await();
    }

    @Override
    public String toString() {
        return "Element{" + description + '}';
    }

    private record WaitResult<T>(T value) {
    }
}
