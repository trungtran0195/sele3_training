package com.pages;

import com.driver.DriverManager;

/** Opens the application pages used by browser tests. */
public final class PageNavigator {

    private static final String DYNAMIC_CONTROLS_URL =
            "https://the-internet.herokuapp.com/dynamic_controls";
    private static final String DROPDOWN_URL = "https://the-internet.herokuapp.com/dropdown";
    private static final String INPUTS_URL = "https://the-internet.herokuapp.com/inputs";
    private static final String HOVERS_URL = "https://the-internet.herokuapp.com/hovers";
    private static final String CHECKBOXES_URL = "https://the-internet.herokuapp.com/checkboxes";

    public void openDynamicControls() {
        DriverManager.getDriver().get(DYNAMIC_CONTROLS_URL);
    }

    public void openDropdown() {
        DriverManager.getDriver().get(DROPDOWN_URL);
    }

    public void openInputs() {
        DriverManager.getDriver().get(INPUTS_URL);
    }

    public void openHovers() {
        DriverManager.getDriver().get(HOVERS_URL);
    }

    public void openCheckboxes() {
        DriverManager.getDriver().get(CHECKBOXES_URL);
    }
}
