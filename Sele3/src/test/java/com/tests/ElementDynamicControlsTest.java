package com.tests;

import com.pages.DynamicControlsPage;
import com.pages.PageNavigator;
import com.test.TestBase;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Exercises Element against DOM controls that are replaced asynchronously in a real browser. */
public class ElementDynamicControlsTest extends TestBase {

    private final PageNavigator navigator = new PageNavigator();
    private final DynamicControlsPage page = new DynamicControlsPage();

    @Test
    public void shouldResolveElementsAgainAfterDynamicDomUpdates() {
        navigator.openDynamicControls();

        Assert.assertTrue(page.isCheckboxDisplayed(), "Checkbox should initially be displayed");
        Assert.assertEquals(page.getCheckboxButtonText(), "Remove");

        page.clickCheckboxButton();
        page.waitUntilCheckboxRemoved();

        Assert.assertFalse(page.isCheckboxDisplayed(), "Checkbox should be removed");

        page.clickCheckboxButton();
        page.waitUntilCheckboxAdded();

        Assert.assertTrue(page.isCheckboxDisplayed(), "Checkbox should be displayed again");

        Assert.assertFalse(page.isTextBoxEnabled(), "Textbox should initially be disabled");

        page.clickTextBoxButton();
        page.waitUntilTextBoxEnabled();

        Assert.assertTrue(page.isTextBoxEnabled(), "Textbox should be enabled");

        page.clickTextBoxButton();
        page.waitUntilTextBoxDisabled();

        Assert.assertFalse(page.isTextBoxEnabled(), "Textbox should be disabled again");
    }
}
