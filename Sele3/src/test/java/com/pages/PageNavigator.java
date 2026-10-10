package com.pages;

import com.driver.DriverManager;

/** Opens the application pages used by browser tests. */
public final class PageNavigator {

    private static final String DYNAMIC_CONTROLS_URL =
            "https://the-internet.herokuapp.com/dynamic_controls";

    public void openDynamicControls() {
        DriverManager.getDriver().get(DYNAMIC_CONTROLS_URL);
    }
}
