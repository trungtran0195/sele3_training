package com.config;

import com.driver.browser.Browser;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.Locale;

final class BrowserTypeAdapter extends TypeAdapter<Browser> {

    @Override
    public void write(JsonWriter out, Browser value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }
        out.value(value.name().toLowerCase(Locale.ROOT));
    }

    @Override
    public Browser read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        try {
            return Browser.from(in.nextString());
        } catch (IllegalArgumentException e) {
            throw new IOException(e.getMessage(), e);
        }
    }
}
