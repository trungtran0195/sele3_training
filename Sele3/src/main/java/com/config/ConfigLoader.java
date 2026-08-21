package com.config;

import com.google.gson.Gson;
import java.io.FileReader;
import java.io.IOException;
import java.lang.module.Configuration;
import java.time.Duration;

public class ConfigLoader {

    public static Configuration fromJsonFile(String jsonFile) {
        Configuration config = load(jsonFile);
        updateConfig(config);
        return config;
    }

    private static void updateConfig(Configuration config) {
        config.setBrowser(System.getProperty(SeleniumKeys.BROWSER, config.getBrowser()));
        config.setHeadless(Boolean.parseBoolean(System.getProperty(SeleniumKeys.HEADLESS, Boolean.toString(config.isHeadless()))));
        config.setRemote(System.getProperty(SeleniumKeys.REMOTE, config.getRemote()));
        config.setBaseUrl(System.getProperty(SeleniumKeys.BASE_URL, config.getBaseUrl()));
        config.setBrowserSize(System.getProperty(SeleniumKeys.BROWSER_SIZE, config.getBrowserSize()));
        config.setStartMaximized(Boolean.parseBoolean(System.getProperty(SeleniumKeys.START_MAXIMIZED, Boolean.toString(config.isStartMaximized()))));
        config.setTimeout(Duration.ofMillis(Long.parseLong(System.getProperty(SeleniumKeys.TIMEOUT, Long.toString(config.getTimeout().toMillis())))));
        config.setPageLoadTimeout(Duration.ofMillis(Long.parseLong(System.getProperty(SeleniumKeys.PAGE_LOAD_TIMEOUT, Long.toString(config.getPageLoadTimeout().toMillis())))));
        config.setPollingInterval(Duration.ofMillis(Long.parseLong(System.getProperty(SeleniumKeys.POLLING_INTERVAL, Long.toString(config.getPollingInterval().toMillis())))));
    }

    private static final Gson gson = new Gson();

    public static Configuration load(String file) {
        try (FileReader reader = new FileReader(file)) {
            return gson.fromJson(reader, Configuration.class);
        } catch (IOException e) {
            throw new RuntimeException("Cannot load config: " + file, e);
        }
    }
}
