package com.report;

import java.util.Map;

public record TestEvent(
        String id,
        String name,
        TestStatus status,
        Throwable error,
        Map<String, Object> attributes) {

    public TestEvent {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
