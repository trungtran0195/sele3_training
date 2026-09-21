package com.element;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;

import java.util.Objects;
import java.util.function.Function;

/** ExpectedCondition with a stable, user-facing timeout description. */
final class ElementCondition<T> implements ExpectedCondition<T> {

    private final String description;
    private final Function<WebDriver, T> delegate;

    ElementCondition(String description, Function<WebDriver, T> delegate) {
        this.description = Objects.requireNonNull(description, "Condition description must not be null");
        this.delegate = Objects.requireNonNull(delegate, "Condition delegate must not be null");
    }

    @Override
    public T apply(WebDriver driver) {
        return delegate.apply(driver);
    }

    @Override
    public String toString() {
        return description;
    }
}
