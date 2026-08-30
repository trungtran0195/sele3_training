package com.driver;

import com.config.Configuration;
import org.openqa.selenium.WebDriver;

public final class DriverContext {

    private DriverContext() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static WebDriver getDriver() {
        return DriverManager.getDriver();
    }

    public static Configuration getConfig() {
        return DriverManager.getConfig();
    }
}
