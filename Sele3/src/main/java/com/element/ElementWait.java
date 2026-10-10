package com.element;

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Per-operation fluent wait. Each instance owns its timeout, message, polling interval, and
 * ignored exceptions, so customization never leaks to another element operation.
 *
 * @param <T> result returned when the condition succeeds
 */
public final class ElementWait<T> {

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

    /**
     * Overrides the timeout for this operation only.
     *
     * @param timeout maximum wait duration
     * @return this wait
     */
    public ElementWait<T> withTimeout(Duration timeout) {
        this.timeout = requirePositive(timeout, "Wait timeout");
        return this;
    }

    /**
     * Overrides the polling interval for this operation only.
     *
     * @param pollingInterval delay between evaluations
     * @return this wait
     */
    public ElementWait<T> pollingEvery(Duration pollingInterval) {
        this.pollingInterval = requirePositive(pollingInterval, "Polling interval");
        return this;
    }

    /**
     * Overrides the timeout message for this operation only.
     *
     * @param timeoutMessage timeout message
     * @return this wait
     */
    public ElementWait<T> withMessage(String timeoutMessage) {
        if (timeoutMessage == null || timeoutMessage.isBlank()) {
            throw new IllegalArgumentException("Wait message must not be blank");
        }
        this.timeoutMessage = timeoutMessage;
        return this;
    }

    /**
     * Adds transient exception types to retry for this operation only.
     * {@code NoSuchElementException} and {@code StaleElementReferenceException} are always retried.
     *
     * @param exceptionTypes additional transient exception types
     * @return this wait
     */
    @SafeVarargs
    public final ElementWait<T> ignoring(Class<? extends Throwable>... exceptionTypes) {
        Objects.requireNonNull(exceptionTypes, "Ignored exceptions must not be null");
        for (Class<? extends Throwable> exceptionType : exceptionTypes) {
            ignoredExceptions.add(Objects.requireNonNull(
                    exceptionType,
                    "Ignored exception type must not be null"));
        }
        return this;
    }

    /**
     * Evaluates the operation until it succeeds or reaches its timeout.
     *
     * @return successful operation result
     * @throws ElementTimeoutException when the timeout expires
     */
    public T await() {
        WebDriverWait wait = new WebDriverWait(
                Objects.requireNonNull(driver, "WebDriver has not been configured"),
                Objects.requireNonNull(timeout, "Wait timeout has not been configured")) {
            @Override
            protected RuntimeException timeoutException(
                    String message,
                    Throwable lastException) {
                return new ElementTimeoutException(
                        Objects.requireNonNullElse(
                                ElementWait.this.timeoutMessage,
                                "Element wait timed out"),
                        lastException);
            }
        };
        wait
                .pollingEvery(Objects.requireNonNull(
                        pollingInterval,
                        "Polling interval has not been configured"))
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class);
        wait.ignoreAll(ignoredExceptions);
        // FluentWait invokes the whole condition again after an ignored exception. Element
        // conditions run their element finder on every invocation, which performs the lookup again.
        return wait.until(condition);
    }

    private static Duration requirePositive(Duration duration, String fieldName) {
        Objects.requireNonNull(duration, fieldName + " must not be null");
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero");
        }
        return duration;
    }

}
