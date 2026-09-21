package com.element;

import org.openqa.selenium.TimeoutException;

/** Indicates that an Element operation did not complete before its configured timeout. */
public class ElementTimeoutException extends TimeoutException {

    public ElementTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
