package com.element;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Interactive;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.List;

import static com.element.ElementTestSupport.driverReturning;
import static com.element.ElementTestSupport.element;
import static com.element.ElementTestSupport.webDriver;
import static com.element.ElementTestSupport.webElement;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Verifies the public lookup and read behavior of {@link Element}. */
public class ElementBehaviorTest {

    @AfterMethod(alwaysRun = true)
    public void cleanUpDriverContext() {
        ElementTestSupport.clearContext();
    }

    @Test
    public void shouldFindAVisibleElementForEveryRead() {
        By locator = By.id("message");
        WebElement webElement = webElement();
        WebDriver driver = webDriver();
        when(driver.findElement(locator)).thenReturn(webElement);
        when(webElement.isDisplayed()).thenReturn(true);
        when(webElement.getText()).thenReturn("Welcome");
        Element element = element(locator, driver);

        Assert.assertEquals(element.getText(), "Welcome");
        Assert.assertEquals(element.getText(), "Welcome");
        verify(driver, times(2)).findElement(locator);
    }

    @Test
    public void shouldHoverOverAVisibleElement() {
        By locator = By.id("menu");
        WebElement webElement = webElement();
        WebDriver driver = webDriver();
        when(driver.findElement(locator)).thenReturn(webElement);
        when(webElement.isDisplayed()).thenReturn(true);

        element(locator, driver).hover();

        verify((Interactive) driver).perform(any());
    }

    @Test
    public void shouldSelectAnOptionByVisibleText() {
        By locator = By.id("country");
        WebElement dropdown = webElement();
        WebElement option = webElement();
        WebDriver driver = webDriver();
        when(driver.findElement(locator)).thenReturn(dropdown);
        when(dropdown.isDisplayed()).thenReturn(true);
        when(dropdown.isEnabled()).thenReturn(true);
        when(dropdown.getTagName()).thenReturn("select");
        when(dropdown.findElements(any(By.class))).thenReturn(List.of(option));
        when(option.isEnabled()).thenReturn(true);
        when(option.isSelected()).thenReturn(false);

        element(locator, driver).selectByVisibleText("Canada");

        verify(option).click();
    }

    @Test
    public void shouldSelectAnOptionByValue() {
        By locator = By.id("country");
        WebElement dropdown = webElement();
        WebElement option = webElement();
        WebDriver driver = webDriver();
        when(driver.findElement(locator)).thenReturn(dropdown);
        when(dropdown.isDisplayed()).thenReturn(true);
        when(dropdown.isEnabled()).thenReturn(true);
        when(dropdown.getTagName()).thenReturn("select");
        when(dropdown.findElements(any(By.class))).thenReturn(List.of(option));
        when(option.isEnabled()).thenReturn(true);
        when(option.isSelected()).thenReturn(false);

        element(locator, driver).selectByValue("ca");

        verify(option).click();
    }

    @Test
    public void shouldCheckAndUncheckByChangingSelection() {
        WebElement checkbox = webElement();
        when(checkbox.isDisplayed()).thenReturn(true);
        when(checkbox.isEnabled()).thenReturn(true);
        when(checkbox.isSelected()).thenReturn(false, true, true, false);
        Element element = element(By.id("terms"), driverReturning(checkbox));

        element.check();
        element.uncheck();

        verify(checkbox, times(2)).click();
    }

    @Test
    public void shouldNotClickWhenCheckboxAlreadyHasRequestedState() {
        WebElement checkbox = webElement();
        when(checkbox.isDisplayed()).thenReturn(true);
        when(checkbox.isEnabled()).thenReturn(true);
        when(checkbox.isSelected()).thenReturn(true, false);
        Element element = element(By.id("terms"), driverReturning(checkbox));

        element.check();
        element.uncheck();

        verify(checkbox, never()).click();
    }

    @Test
    public void shouldFindAVisibleChildInsideTheCurrentParent() {
        By parentLocator = By.id("form");
        By childLocator = By.className("message");
        WebElement parent = webElement();
        WebElement child = webElement();
        WebDriver driver = webDriver();
        when(driver.findElement(parentLocator)).thenReturn(parent);
        when(parent.findElements(childLocator)).thenReturn(List.of(child));
        when(child.isDisplayed()).thenReturn(true);
        when(child.getText()).thenReturn("Nested");

        Assert.assertEquals(element(parentLocator, driver).child(childLocator).getText(), "Nested");
        verify(driver).findElement(parentLocator);
    }

    @Test
    public void shouldNotAccessDriverWhileDefiningAChild() {
        By parentLocator = By.id("form");
        WebDriver driver = webDriver();
        Element parent = element(parentLocator, driver);

        Element child = parent.child(By.id("message"));

        Assert.assertNotNull(child);
        verify(driver, never()).findElement(parentLocator);
    }

    @Test
    public void shouldReturnNullWhenAttributeIsAbsent() {
        WebElement webElement = webElement();
        Element element = element(By.id("optional"), driverReturning(webElement));

        Assert.assertNull(element.getAttribute("data-value"));
    }

    @Test
    public void shouldReadPropertiesWithoutRequiringVisibility() {
        WebElement hiddenElement = webElement();
        when(hiddenElement.getAttribute("value")).thenReturn("hidden-value");
        when(hiddenElement.isEnabled()).thenReturn(true);
        when(hiddenElement.isSelected()).thenReturn(true);
        Element element = element(By.id("hidden-control"), driverReturning(hiddenElement));

        Assert.assertEquals(element.getAttribute("value"), "hidden-value");
        Assert.assertTrue(element.isEnabled());
        Assert.assertTrue(element.isSelected());
        verify(hiddenElement, never()).isDisplayed();
    }

    @Test
    public void shouldReadHiddenChildStateWithoutRequiringVisibility() {
        By parentLocator = By.id("form");
        By childLocator = By.id("hidden-control");
        WebElement parent = webElement();
        WebElement hiddenChild = webElement();
        WebDriver driver = webDriver();
        when(driver.findElement(parentLocator)).thenReturn(parent);
        when(parent.findElements(childLocator)).thenReturn(List.of(hiddenChild));
        when(hiddenChild.isEnabled()).thenReturn(true);
        when(hiddenChild.isSelected()).thenReturn(true);
        Element child = element(parentLocator, driver).child(childLocator);

        Assert.assertTrue(child.isEnabled());
        Assert.assertTrue(child.isSelected());
        verify(hiddenChild, never()).isDisplayed();
    }

    @Test
    public void shouldCheckDisplayedStateWithoutWaiting() {
        By locator = By.id("status");
        WebElement hiddenElement = webElement();
        WebDriver driver = webDriver();
        when(driver.findElement(locator)).thenReturn(hiddenElement);
        when(hiddenElement.isDisplayed()).thenReturn(false);

        Assert.assertFalse(element(locator, driver).isDisplayed());
        verify(driver).findElement(locator);
    }

    @Test
    public void shouldReturnFalseWhenDisplayedElementDoesNotExist() {
        By locator = By.id("missing");
        WebDriver driver = webDriver();
        when(driver.findElement(locator)).thenThrow(new NoSuchElementException("Not found"));

        Assert.assertFalse(element(locator, driver).isDisplayed());
        verify(driver).findElement(locator);
    }

    @Test
    public void shouldFindElementAgainWhenCheckingAStaleElement() {
        By locator = By.id("replaced");
        WebElement staleElement = webElement();
        WebElement currentElement = webElement();
        WebDriver driver = webDriver();
        when(driver.findElement(locator)).thenReturn(staleElement, currentElement);
        when(staleElement.isDisplayed())
                .thenThrow(new StaleElementReferenceException("DOM changed"));
        when(currentElement.isDisplayed()).thenReturn(true);

        Assert.assertTrue(element(locator, driver).isDisplayed());
        verify(driver, times(2)).findElement(locator);
    }

    @Test
    public void shouldPropagateStaleWhenImmediateRetryAlsoFails() {
        By locator = By.id("unstable");
        WebElement firstElement = webElement();
        WebElement secondElement = webElement();
        WebDriver driver = webDriver();
        when(driver.findElement(locator)).thenReturn(firstElement, secondElement);
        when(firstElement.isDisplayed())
                .thenThrow(new StaleElementReferenceException("First DOM change"));
        when(secondElement.isDisplayed())
                .thenThrow(new StaleElementReferenceException("Second DOM change"));

        Assert.expectThrows(
                StaleElementReferenceException.class,
                () -> element(locator, driver).isDisplayed());
        verify(driver, times(2)).findElement(locator);
    }
}
