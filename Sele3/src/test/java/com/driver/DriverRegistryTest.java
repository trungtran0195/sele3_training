package com.driver;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Set;

public class DriverRegistryTest {

    @Test
    public void shouldDiscoverBuiltInProviders() {
        Assert.assertEquals(
                DriverRegistry.providers().keySet(),
                Set.of("chrome", "firefox", "edge"));
    }
}
