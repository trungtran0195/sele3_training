package com.element;

import org.openqa.selenium.By;

import java.util.function.Function;

/** Supported Selenium locator strategies. */
public enum LocatorType {

    ID(By::id),
    NAME(By::name),
    CLASS_NAME(By::className),
    CSS(By::cssSelector),
    XPATH(By::xpath),
    TAG_NAME(By::tagName),
    LINK_TEXT(By::linkText),
    PARTIAL_LINK_TEXT(By::partialLinkText);

    private final Function<String, By> locatorFactory;

    LocatorType(Function<String, By> locatorFactory) {
        this.locatorFactory = locatorFactory;
    }

    public By toBy(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Locator value must not be blank");
        }
        return locatorFactory.apply(value);
    }
}
