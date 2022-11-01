package com.styra.run;

import com.fasterxml.jackson.jr.ob.JSON;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class DefaultJson implements Json {
    @Override
    public String from(Object value) throws IOException {
        return JSON.std.asString(value);
    }

    @Override
    public Map<String, ?> toMap(String source) throws IOException {
        if (source == null) {
            return null;
        }

        return JSON.std.mapFrom(source);
    }

    @Override
    public <T> List<T> toList(Class<T> type, String source) throws IOException {
        if (source == null) {
            return null;
        }

        return JSON.std.listOfFrom(type, source);
    }

    @Override
    public <T> T to(Class<T> type, String source) throws IOException {
        if (source == null) {
            return null;
        }

        return JSON.std.beanFrom(type, source);
    }
}
