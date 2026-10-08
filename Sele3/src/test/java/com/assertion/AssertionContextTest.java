package com.assertion;

import com.config.ConfigLoader;
import com.config.Configuration;
import com.driver.DriverManager;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Path;

public class AssertionContextTest {

    private static final Configuration CONFIGURATION = ConfigLoader.fromJsonFile(Path.of(
            "src", "test", "resources", "config", "defaults.json").toString());

    @BeforeMethod
    public void setUp() {
        DriverManager.setConfig(CONFIGURATION);
    }

    @AfterMethod(alwaysRun = true)
    public void cleanUp() {
        try {
            AssertionContext.finish();
        } finally {
            DriverManager.cleanup();
        }
    }

    @Test
    public void shouldCreateAndFinishAssertionSessionAutomatically() {
        AssertionContext.current().softTrue(false, "soft failure");

        AssertionError failure = Assert.expectThrows(AssertionError.class, AssertionContext::finish);

        Assert.assertEquals(failure.getSuppressed().length, 1);
        Assert.assertNotNull(AssertionContext.current());
    }

    @Test
    public void shouldReuseSessionOnTheSameThread() {
        AssertionManager first = AssertionContext.current();

        Assert.assertSame(AssertionContext.current(), first);
    }

    @Test
    public void shouldAllowFinishWhenSetupFailedBeforeSessionStarted() {
        AssertionContext.finish();

        Assert.assertNotNull(AssertionContext.current());
    }
}
