package com.assertion;

import com.config.ConfigLoader;
import com.config.Configuration;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Path;

public class AssertionContextTest {

    private static final Configuration CONFIGURATION = ConfigLoader.fromJsonFile(Path.of(
            "src", "test", "resources", "config", "defaults.json").toString());

    @Test
    public void shouldStartAndFinishAssertionSession() {
        AssertionContext.start(CONFIGURATION);
        AssertionContext.current().softTrue(false, "soft failure");

        AssertionError failure = Assert.expectThrows(AssertionError.class, AssertionContext::finish);

        Assert.assertEquals(failure.getSuppressed().length, 1);
        Assert.expectThrows(IllegalStateException.class, AssertionContext::current);
    }

    @Test
    public void shouldRejectAnotherSessionOnTheSameThread() {
        AssertionContext.start(CONFIGURATION);
        try {
            Assert.expectThrows(
                    IllegalStateException.class,
                    () -> AssertionContext.start(CONFIGURATION));
        } finally {
            AssertionContext.finish();
        }
    }

    @Test
    public void shouldAllowFinishWhenSetupFailedBeforeSessionStarted() {
        AssertionContext.finish();

        Assert.expectThrows(IllegalStateException.class, AssertionContext::current);
    }
}
