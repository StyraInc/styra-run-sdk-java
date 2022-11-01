package com.styra.run;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface SerializableAsMap {
    Map<String, ?> toMap();

    static Object serialize(Object value) {
        if (value instanceof SerializableAsMap) {
            return ((SerializableAsMap) value).toMap();
        } else if (value instanceof List) {
            return ((List<?>) value).stream()
                    .map(SerializableAsMap::serialize)
                    .collect(Collectors.toList());
        } else if (value instanceof Map) {
            return ((Map<?, ?>) value).entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            (e) -> serialize(e.getValue())));
        }
        return value;
    }
}
