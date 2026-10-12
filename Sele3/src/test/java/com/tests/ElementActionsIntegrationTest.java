package com.tests;

import com.element.Element;
import com.pages.PageNavigator;
import com.test.TestBase;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Exercises Element actions against native controls in a real browser. */
public class ElementActionsIntegrationTest extends TestBase {

    private final PageNavigator navigator = new PageNavigator();

    @Test
    public void shouldSelectDropdownOptionsByTextValueAndIndex() {
        navigator.openDropdown();
        Element dropdown = Element.of(By.id("dropdown"));

        dropdown.scrollIntoView();
        dropdown.selectByVisibleText("Option 2");
        Assert.assertEquals(dropdown.getAttribute("value"), "2");

        dropdown.selectByValue("1");
        Assert.assertEquals(dropdown.getAttribute("value"), "1");

        dropdown.selectByIndex(2);
        Assert.assertEquals(dropdown.getAttribute("value"), "2");
    }

    @Test
    public void shouldScrollToInputAndSetItsValue() {
        navigator.openInputs();
        Element input = Element.of(By.cssSelector("input[type='number']"));

        input.scrollIntoView();
        input.setValue("42");

        Assert.assertEquals(input.getAttribute("value"), "42");
    }

    @Test
    public void shouldRevealCaptionWhenHoveringOverFigure() {
        navigator.openHovers();
        Element figure = Element.of(By.cssSelector(".figure"));
        Element caption = Element.of(By.cssSelector(".figure .figcaption"));

        Assert.assertFalse(caption.isDisplayed());
        figure.hover();

        Assert.assertTrue(caption.isDisplayed());
    }

    @Test
    public void shouldCheckAndUncheckCheckbox() {
        navigator.openCheckboxes();
        Element checkbox = Element.of(By.cssSelector("input[type='checkbox']"));

        checkbox.check();
        Assert.assertTrue(checkbox.isSelected());

        checkbox.uncheck();
        Assert.assertFalse(checkbox.isSelected());
    }
}
