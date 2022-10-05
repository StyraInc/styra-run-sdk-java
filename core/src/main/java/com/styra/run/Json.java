package com.styra.run;

import com.fasterxml.jackson.jr.ob.JSON;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// TODO: Make pluggable
public class Json {
    public String from(Object value) throws IOException {
        return JSON.std.asString(value);
    }

    public Map<String, ?> toMap(String source) throws IOException {
        if (source == null) {
            return null;
        }

        return JSON.std.mapFrom(source);
    }

    public Optional<Map<String, ?>> toOptionalMap(String source) {
        if (source == null) {
            return Optional.empty();
        }

        try {
            return Optional.ofNullable(JSON.std.mapFrom(source));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public <T> List<T> toList(Class<T> type, String source) throws IOException {
        if (source == null) {
            return null;
        }

        return JSON.std.listOfFrom(type, source);
    }

    public <T> Optional<List<T>> toOptionalList(Class<T> type, String source) {
        if (source == null) {
            return Optional.empty();
        }

        try {
            return Optional.ofNullable(JSON.std.listOfFrom(type, source));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public <T> T to(Class<T> type, String source) throws IOException {
        if (source == null) {
            return null;
        }

        return JSON.std.beanFrom(type, source);
    }

    public Object toAny(String source) {
        if (source == null) {
            return null;
        }

        try {
            return JSON.std.anyFrom(source);
        } catch (IOException e) {
            return source;
        }
    }
}
