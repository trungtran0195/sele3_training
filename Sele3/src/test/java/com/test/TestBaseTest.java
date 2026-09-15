package com.test;

import com.config.ConfigurationException;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestBaseTest {

    @Test
    public void shouldPreferSuiteConfigFile() {
        Assert.assertEquals(
                TestBase.resolveConfigFile("suite.json", "property.json", "environment.json"),
                "suite.json");
    }

    @Test
    public void shouldUseSystemPropertyWhenSuiteValueIsBlank() {
        Assert.assertEquals(
                TestBase.resolveConfigFile("", "property.json", "environment.json"),
                "property.json");
    }

    @Test
    public void shouldUseEnvironmentWhenHigherPriorityValuesAreBlank() {
        Assert.assertEquals(
                TestBase.resolveConfigFile("", "", "environment.json"),
                "environment.json");
    }

    @Test(expectedExceptions = ConfigurationException.class)
    public void shouldRequireAConfigFile() {
        TestBase.resolveConfigFile("", "", "");
    }
}
