package com.driver;

import com.config.Configuration;
import org.openqa.selenium.WebDriver;

/** Extension point for browser-specific WebDriver implementations. */
public interface DriverProvider {

    /** @return unique browser name used in configuration */
    String name();

    /**
     * @param configuration validated framework configuration
     * @return created driver
     */
    WebDriver create(Configuration configuration);
}
