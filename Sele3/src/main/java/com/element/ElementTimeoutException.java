package com.element;

import org.openqa.selenium.TimeoutException;

/** Indicates that an Element operation did not complete before its configured timeout. */
public class ElementTimeoutException extends TimeoutException {

    private final String timeoutMessage;

    public ElementTimeoutException(String timeoutMessage, Throwable cause) {
        super(timeoutMessage, cause);
        this.timeoutMessage = timeoutMessage;
    }

    @Override
    public String getMessage() {
        return timeoutMessage;
    }
}
