package com.report;

public interface Reporter {
    default void onStart(TestEvent event) {
    }

    default void onSuccess(TestEvent event) {
    }

    default void onFailure(TestEvent event) {
    }

    default void onSkipped(TestEvent event) {
    }

    default void attach(String name, String mediaType, byte[] content) {
    }

    default void flush() {
    }
}
