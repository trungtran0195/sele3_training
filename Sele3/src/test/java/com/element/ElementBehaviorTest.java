package com.element;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.element.ElementTestSupport.defaultValue;
import static com.element.ElementTestSupport.driverReturning;
import static com.element.ElementTestSupport.element;
import static com.element.ElementTestSupport.webDriver;
import static com.element.ElementTestSupport.webElement;

/** Verifies the public lookup and read behavior of {@link Element}. */
public class ElementBehaviorTest {

    @AfterMethod(alwaysRun = true)
    public void cleanUpDriverManager() {
        ElementTestSupport.clearContext();
    }

    @Test
    public void shouldFindAVisibleElementForEveryRead() {
        AtomicInteger finds = new AtomicInteger();
        WebElement webElement = webElement((proxy, method, args) -> switch (method.getName()) {
            case "isDisplayed" -> true;
            case "getText" -> "Welcome";
            default -> defaultValue(method);
        });
        WebDriver driver = webDriver((proxy, method, args) -> {
            if (method.getName().equals("findElement")) {
                finds.incrementAndGet();
                return webElement;
            }
            return defaultValue(method);
        });
        Element element = element(By.id("message"), driver);

        Assert.assertEquals(element.getText(), "Welcome");
        Assert.assertEquals(element.getText(), "Welcome");
        Assert.assertEquals(finds.get(), 2);
    }

    @Test
    public void shouldFindAVisibleChildInsideTheCurrentParent() {
        AtomicInteger parentFinds = new AtomicInteger();
        WebElement child = webElement((proxy, method, args) -> switch (method.getName()) {
            case "isDisplayed" -> true;
            case "getText" -> "Nested";
            default -> defaultValue(method);
        });
        WebElement parent = webElement((proxy, method, args) ->
                method.getName().equals("findElements")
                        ? List.of(child)
                        : defaultValue(method));
        WebDriver driver = webDriver((proxy, method, args) -> {
            if (method.getName().equals("findElement")) {
                parentFinds.incrementAndGet();
                return parent;
            }
            return defaultValue(method);
        });
        Element form = element(By.id("form"), driver);

        Assert.assertEquals(form.child(By.className("message")).getText(), "Nested");
        Assert.assertEquals(parentFinds.get(), 1);
    }

    @Test
    public void shouldNotAccessDriverWhileDefiningAChild() {
        WebDriver driver = webDriver((proxy, method, args) -> {
            if (method.getName().equals("findElement")) {
                throw new AssertionError("Driver was accessed while defining an element");
            }
            return defaultValue(method);
        });
        Element parent = element(By.id("form"), driver);

        Element child = parent.child(By.id("message"));

        Assert.assertNotNull(child);
    }

    @Test
    public void shouldReturnNullWhenAttributeIsAbsent() {
        WebElement webElement = webElement((proxy, method, args) -> defaultValue(method));
        Element element = element(By.id("optional"), driverReturning(webElement));

        Assert.assertNull(element.getAttribute("data-value"));
    }

    @Test
    public void shouldReadPropertiesWithoutRequiringVisibility() {
        WebElement hiddenElement = webElement((proxy, method, args) -> switch (method.getName()) {
            case "isDisplayed" -> false;
            case "getAttribute" -> "hidden-value";
            case "isEnabled", "isSelected" -> true;
            default -> defaultValue(method);
        });
        Element element = element(By.id("hidden-control"), driverReturning(hiddenElement));

        Assert.assertEquals(element.getAttribute("value"), "hidden-value");
        Assert.assertTrue(element.isEnabled());
        Assert.assertTrue(element.isSelected());
    }
}
