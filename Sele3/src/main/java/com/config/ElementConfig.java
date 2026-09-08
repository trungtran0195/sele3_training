package com.config;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;

import java.time.Duration;

@Value
@Builder(access = AccessLevel.PACKAGE)
public class ElementConfig {
    Duration timeout;
    Duration pollingInterval;
}
