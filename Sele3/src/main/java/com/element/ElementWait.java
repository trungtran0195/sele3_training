package com.element;

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/** Wait operations shared by every Element action. */
final class ElementWait {

    private final WebDriverWait wait;

    ElementWait(WebDriver driver, Duration timeout, Duration pollingInterval) {
        WebDriverWait configuredWait = new WebDriverWait(driver, timeout);
        configuredWait
                .pollingEvery(pollingInterval)
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class);
        wait = configuredWait;
    }

    <T> T until(ExpectedCondition<T> condition) {
        try {
            return wait.until(condition);
        } catch (TimeoutException e) {
            throw new ElementTimeoutException(condition.toString(), e);
        }
    }
}
