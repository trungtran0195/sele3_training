package com.config;

public final class ConfigKey {

    private ConfigKey() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static final String BROWSER = "browser";
    public static final String HEADLESS = "headless";
    public static final String REMOTE = "remote";
    public static final String BASE_URL = "baseUrl";
    public static final String START_MAXIMIZED = "startMaximized";
    public static final String BROWSER_SIZE = "browserSize";
    public static final String TIMEOUT = "timeout";
    public static final String PAGE_LOAD_TIMEOUT = "pageLoadTimeout";
    public static final String POLLING_INTERVAL = "pollingInterval";
    public static final String PAGE_LOAD_STRATEGY = "pageLoadStrategy";
}
