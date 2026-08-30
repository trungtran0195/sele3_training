package com.driver.browser;

import java.util.Locale;

public enum Browser {
    CHROME,
    FIREFOX,
    EDGE;

    public static Browser from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Browser must not be blank");
        }

        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "chrome", "chromium" -> CHROME;
            case "firefox" -> FIREFOX;
            case "edge", "msedge" -> EDGE;
            default -> throw new IllegalArgumentException("Unsupported browser: " + value);
        };
    }
}
