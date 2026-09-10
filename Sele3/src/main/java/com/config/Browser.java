package com.config;

import com.google.gson.annotations.SerializedName;

import java.util.Locale;

public enum Browser {
    @SerializedName("chrome")
    CHROME,

    @SerializedName("firefox")
    FIREFOX,

    @SerializedName("edge")
    EDGE;

    public static Browser from(String value) {
        if (value == null || value.isBlank()) {
            throw new ConfigurationException("browser must not be blank");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException("Unsupported browser: " + value, e);
        }
    }
}
