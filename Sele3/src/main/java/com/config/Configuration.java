package com.config;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.PageLoadStrategy;

import java.time.Duration;

@Value
@Builder(access = AccessLevel.PACKAGE)
public class Configuration {

    Browser browser;
    boolean headless;
    String baseUrl;

    @ToString.Exclude
    String remote;

    boolean startMaximized;
    String browserSize;
    Duration timeout;
    Duration pageLoadTimeout;
    Duration pollingInterval;
    PageLoadStrategy pageLoadStrategy;

    @ToString.Exclude
    Capabilities capabilities;

    public boolean isRemote() {
        return remote != null && !remote.isBlank();
    }
}
