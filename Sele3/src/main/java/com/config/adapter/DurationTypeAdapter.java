package com.config.adapter;

import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.Duration;

public final class DurationTypeAdapter extends TypeAdapter<Duration> {

    @Override
    public void write(JsonWriter out, Duration value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }
        out.value(value.toMillis());
    }

    @Override
    public Duration read(JsonReader in) throws IOException {
        JsonToken token = in.peek();
        if (token == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        try {
            if (token == JsonToken.NUMBER) {
                return Duration.ofMillis(in.nextLong());
            }
            if (token == JsonToken.STRING) {
                String value = in.nextString().trim();
                return value.matches("-?\\d+")
                        ? Duration.ofMillis(Long.parseLong(value))
                        : Duration.parse(value);
            }
        } catch (RuntimeException e) {
            throw new JsonParseException("Invalid duration value", e);
        }
        throw new JsonParseException("Duration must be milliseconds or an ISO-8601 string");
    }
}
