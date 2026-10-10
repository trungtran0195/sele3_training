package com.pages;

import com.element.Element;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.time.Duration;

/** Page object for the dynamic controls Selenium example. */
public final class DynamicControlsPage {

    private static final Duration DYNAMIC_CONTROL_TIMEOUT = Duration.ofSeconds(10);

    private final Element checkbox = Element.of(By.id("checkbox"));
    private final Element checkboxButton = Element.of(By.cssSelector("#checkbox-example button"));
    private final Element textBox = Element.of(By.cssSelector("#input-example input"));
    private final Element textBoxButton = Element.of(By.cssSelector("#input-example button"));

    public void clickCheckboxButton() {
        checkboxButton.click();
    }

    public void clickTextBoxButton() {
        textBoxButton.click();
    }

    public boolean isCheckboxDisplayed() {
        return checkbox.isDisplayed();
    }

    public boolean isTextBoxEnabled() {
        return textBox.isEnabled();
    }

    public String getCheckboxButtonText() {
        return checkboxButton.getText();
    }

    public void waitUntilCheckboxRemoved() {
        checkboxButton
                .waitFor(
                        "Checkbox should be removed",
                        element -> "Add".equals(element.getText()))
                .withTimeout(DYNAMIC_CONTROL_TIMEOUT)
                .await();
    }

    public void waitUntilCheckboxAdded() {
        checkboxButton
                .waitFor(
                        "Checkbox should be added",
                        element -> "Remove".equals(element.getText()))
                .withTimeout(DYNAMIC_CONTROL_TIMEOUT)
                .await();
    }

    public void waitUntilTextBoxEnabled() {
        textBoxButton
                .waitFor(
                        "Textbox button should become Disable",
                        element -> "Disable".equals(element.getText()))
                .withTimeout(DYNAMIC_CONTROL_TIMEOUT)
                .await();
        textBox
                .waitFor("Textbox should be enabled", WebElement::isEnabled)
                .withTimeout(DYNAMIC_CONTROL_TIMEOUT)
                .await();
    }

    public void waitUntilTextBoxDisabled() {
        textBoxButton
                .waitFor(
                        "Textbox button should become Enable",
                        element -> "Enable".equals(element.getText()))
                .withTimeout(DYNAMIC_CONTROL_TIMEOUT)
                .await();
        textBox
                .waitFor("Textbox should be disabled", element -> !element.isEnabled())
                .withTimeout(DYNAMIC_CONTROL_TIMEOUT)
                .await();
    }
}
