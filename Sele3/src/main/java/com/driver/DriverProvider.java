package com.driver;

import com.config.DriverConfig;
import com.extension.NamedProvider;
import org.openqa.selenium.WebDriver;

public interface DriverProvider extends NamedProvider {
    WebDriver create(DriverConfig config);
}
