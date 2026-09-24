package com.element;

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/** Fluent wait configuration shared by every Element action. */
final class ElementWait<T> {

    private final Function<WebDriver, T> condition;
    private WebDriver driver;
    private Duration timeout;
    private Duration pollingInterval;
    private String timeoutMessage;
    private final List<Class<? extends Throwable>> ignoredExceptions = new ArrayList<>();

    private ElementWait(Function<WebDriver, T> condition) {
        this.condition = Objects.requireNonNull(condition, "Wait condition must not be null");
    }

    static <T> ElementWait<T> forCondition(Function<WebDriver, T> condition) {
        return new ElementWait<>(condition);
    }

    ElementWait<T> usingDriver(WebDriver driver) {
        this.driver = Objects.requireNonNull(driver, "WebDriver must not be null");
        return this;
    }

    ElementWait<T> withTimeout(Duration timeout) {
        this.timeout = Objects.requireNonNull(timeout, "Wait timeout must not be null");
        return this;
    }

    ElementWait<T> pollingEvery(Duration pollingInterval) {
        this.pollingInterval = Objects.requireNonNull(
                pollingInterval,
                "Polling interval must not be null");
        return this;
    }

    ElementWait<T> withMessage(String timeoutMessage) {
        if (timeoutMessage == null || timeoutMessage.isBlank()) {
            throw new IllegalArgumentException("Wait message must not be blank");
        }
        this.timeoutMessage = timeoutMessage;
        return this;
    }

    @SafeVarargs
    final ElementWait<T> ignoring(Class<? extends Throwable>... exceptionTypes) {
        Objects.requireNonNull(exceptionTypes, "Ignored exceptions must not be null");
        for (Class<? extends Throwable> exceptionType : exceptionTypes) {
            ignoredExceptions.add(Objects.requireNonNull(
                    exceptionType,
                    "Ignored exception type must not be null"));
        }
        return this;
    }

    T await() {
        WebDriverWait wait = new WebDriverWait(
                Objects.requireNonNull(driver, "WebDriver has not been configured"),
                Objects.requireNonNull(timeout, "Wait timeout has not been configured"));
        wait
                .pollingEvery(Objects.requireNonNull(
                        pollingInterval,
                        "Polling interval has not been configured"))
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class);
        wait.ignoreAll(ignoredExceptions);
        try {
            return wait.until(condition);
        } catch (TimeoutException e) {
            throw new ElementTimeoutException(
                    Objects.requireNonNullElse(timeoutMessage, "Element wait timed out"),
                    e);
        }
    }

}
