package com.element;

import com.config.Configuration;
import com.driver.DriverContext;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Locator-based element that resolves a fresh WebElement for every operation.
 */
public final class Element {

    private final Function<WebDriver, WebElement> resolver;
    private final Supplier<WebDriver> driverSupplier;
    private final Supplier<Configuration> configurationSupplier;
    private final String description;
    private final String timeoutMessage;

    private Element(
            Function<WebDriver, WebElement> resolver,
            Supplier<WebDriver> driverSupplier,
            Supplier<Configuration> configurationSupplier,
            String description,
            String timeoutMessage) {
        this.resolver = Objects.requireNonNull(resolver, "Element resolver must not be null");
        this.driverSupplier = Objects.requireNonNull(driverSupplier, "Driver supplier must not be null");
        this.configurationSupplier = Objects.requireNonNull(
                configurationSupplier,
                "Configuration supplier must not be null");
        this.description = Objects.requireNonNull(description, "Element description must not be null");
        this.timeoutMessage = timeoutMessage;
    }

    public static Element of(By locator) {
        return create(locator, DriverContext::getDriver, DriverContext::getConfig);
    }

    public static Element of(LocatorType type, String value) {
        return of(Objects.requireNonNull(type, "Locator type must not be null").toBy(value));
    }

    public static Element find(By locator) {
        Element element = of(locator);
        element.resolveVisible();
        return element;
    }

    public static Element find(LocatorType type, String value) {
        Element element = of(type, value);
        element.resolveVisible();
        return element;
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
                null);
    }

    public Element findElement(By childLocator) {
        Objects.requireNonNull(childLocator, "Child locator must not be null");
        Element child = new Element(
                driver -> resolver.apply(driver).findElement(childLocator),
                driverSupplier,
                configurationSupplier,
                description + " -> " + childLocator,
                null);
        child.resolveVisible();
        return child;
    }

    public Element findElement(LocatorType type, String value) {
        return findElement(Objects.requireNonNull(type, "Locator type must not be null").toBy(value));
    }

    public Element named(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Element name must not be blank");
        }
        return new Element(resolver, driverSupplier, configurationSupplier, name, timeoutMessage);
    }

    public Element withTimeoutMessage(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Timeout message must not be blank");
        }
        return new Element(resolver, driverSupplier, configurationSupplier, description, message);
    }

    public void click() {
        waitForCurrentThread().until(condition("click " + description, currentDriver -> {
            WebElement element = findVisible(currentDriver);
            if (element == null || !element.isEnabled()) {
                return null;
            }
            element.click();
            return Boolean.TRUE;
        }));
    }

    public void sendKeys(CharSequence... values) {
        Objects.requireNonNull(values, "Values must not be null");
        performWhenVisible("send keys to " + description, element -> element.sendKeys(values));
    }

    public void clear() {
        performWhenVisible("clear " + description, WebElement::clear);
    }

    public String getText() {
        return readWhenVisible("read text from " + description, WebElement::getText);
    }

    public String getAttribute(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Attribute name must not be blank");
        }
        return readWhenVisible(
                "read attribute '" + name + "' from " + description,
                element -> element.getAttribute(name));
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
        return waitForCurrentThread().until(condition("find visible " + description, this::findVisible));
    }

    private WebElement findVisible(WebDriver driver) {
        WebElement element = resolver.apply(driver);
        return element.isDisplayed() ? element : null;
    }

    private void performWhenVisible(String operation, Consumer<WebElement> action) {
        waitForCurrentThread().until(visibleAction(operation, action));
    }

    private ExpectedCondition<Boolean> visibleAction(String operation, Consumer<WebElement> action) {
        return condition(operation, currentDriver -> {
            WebElement element = findVisible(currentDriver);
            if (element == null) {
                return null;
            }
            action.accept(element);
            return Boolean.TRUE;
        });
    }

    private <T> T readWhenVisible(String operation, Function<WebElement, T> reader) {
        return waitForCurrentThread().until(visibleRead(operation, reader)).value();
    }

    private <T> ExpectedCondition<Evaluation<T>> visibleRead(
            String operation,
            Function<WebElement, T> reader) {
        return condition(operation, currentDriver -> {
            WebElement element = findVisible(currentDriver);
            return element == null ? null : new Evaluation<>(reader.apply(element));
        });
    }

    private <T> ExpectedCondition<T> condition(
            String defaultMessage,
            Function<WebDriver, T> delegate) {
        String message = timeoutMessage == null ? defaultMessage : timeoutMessage;
        return new ElementCondition<>(message, delegate);
    }

    private ElementWait waitForCurrentThread() {
        WebDriver driver = driverSupplier.get();
        if (driver == null) {
            throw new IllegalStateException("WebDriver has not been initialized for this thread");
        }

        Configuration configuration = configurationSupplier.get();
        if (configuration == null) {
            throw new IllegalStateException("Configuration has not been initialized for this thread");
        }

        return new ElementWait(
                driver,
                configuration.getTimeout(),
                configuration.getPollingInterval());
    }

    @Override
    public String toString() {
        return "Element{" + description + '}';
    }

    private record Evaluation<T>(T value) {
    }
}
