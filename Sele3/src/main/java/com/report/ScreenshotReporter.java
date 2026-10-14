package com.report;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Saves failure screenshots to the configured report output directory. */
public final class ScreenshotReporter implements Reporter {

    public static final String OUTPUT_DIRECTORY_PROPERTY = "screenshot.output";
    private static final Path DEFAULT_OUTPUT_DIRECTORY = Path.of("target", "screenshots");

    @Override
    public void report(TestEvent event) {
        // This reporter handles attachments only.
    }

    @Override
    public void attachScreenshot(TestEvent event, byte[] screenshot) throws IOException {
        Objects.requireNonNull(event, "Test event must not be null");
        Objects.requireNonNull(screenshot, "Screenshot must not be null");

        Path outputDirectory = Path.of(System.getProperty(
                OUTPUT_DIRECTORY_PROPERTY,
                DEFAULT_OUTPUT_DIRECTORY.toString()));
        Files.createDirectories(outputDirectory);
        Files.write(outputDirectory.resolve(fileName(event.testName())), screenshot);
    }

    private static String fileName(String testName) {
        String safeTestName = testName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return safeTestName + "-" + System.nanoTime() + ".png";
    }
}
