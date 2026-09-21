package com.element;

import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class LocatorTypeTest {

    @DataProvider
    public Object[][] locators() {
        return new Object[][]{
                {LocatorType.ID, "user", By.id("user")},
                {LocatorType.NAME, "user", By.name("user")},
                {LocatorType.CLASS_NAME, "field", By.className("field")},
                {LocatorType.CSS, ".field", By.cssSelector(".field")},
                {LocatorType.XPATH, "//input", By.xpath("//input")},
                {LocatorType.TAG_NAME, "input", By.tagName("input")},
                {LocatorType.LINK_TEXT, "Home", By.linkText("Home")},
                {LocatorType.PARTIAL_LINK_TEXT, "Hom", By.partialLinkText("Hom")}
        };
    }

    @Test(dataProvider = "locators")
    public void shouldCreateSeleniumLocator(LocatorType type, String value, By expected) {
        Assert.assertEquals(type.toBy(value), expected);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectBlankLocator() {
        LocatorType.XPATH.toBy(" ");
    }
}
