package com.test;

import com.config.ConfigurationLoader;
import com.config.Configuration;
import com.driver.DriverManager;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

public abstract class TestBase {

    @BeforeMethod(alwaysRun = true)
    public final void setUp() {
        Configuration configuration = ConfigurationLoader.load();
        DriverManager.setConfig(configuration);
        DriverManager.createDriver();
    }

    @AfterMethod(alwaysRun = true)
    public final void tearDown() {
        DriverManager.cleanup();
    }
}
