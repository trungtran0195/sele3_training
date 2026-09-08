package com.config;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;
import org.openqa.selenium.PageLoadStrategy;

import java.time.Duration;
import java.util.Map;

@Value
@Builder(access = AccessLevel.PACKAGE)
public class DriverConfig {
    String browser;
    boolean headless;
    String baseUrl;

    @ToString.Exclude
    String remoteUrl;

    boolean startMaximized;
    String browserSize;
    Duration pageLoadTimeout;
    PageLoadStrategy pageLoadStrategy;

    @ToString.Exclude
    Map<String, Object> options;

    public boolean isRemote() {
        return remoteUrl != null && !remoteUrl.isBlank();
    }
}
