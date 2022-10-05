package com.styra.run.utils;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class Types {
    public static <T, E extends Throwable> T cast(Class<T> type, Object value, Supplier<E> exceptionSupplier) throws E {
        try {
            return type.cast(value);
        } catch (ClassCastException e) {
            throw exceptionSupplier.get();
        }
    }

    public static <T> T safeCast(Class<T> type, Object value) {
        if (type.isAssignableFrom(value.getClass())) {
            return type.cast(value);
        }
        return null;
    }

    public static <K, V> Map<K, V> castMap(Class<K> keyType, Class<V> valueType, Map<?, ?> map) {
        return map.entrySet().stream()
                .collect(Collectors.toMap(
                        (e) -> keyType.cast(e.getKey()),
                        (e) -> valueType.cast(e.getValue())));
    }

    public static <V> Map<String, V> castMap(Class<V> valueType, Map<?, ?> map) {
        return map.entrySet().stream()
                .collect(Collectors.toMap(
                        (e) -> e.getKey().toString(),
                        (e) -> valueType.cast(e.getValue())));
    }

    public static <T> List<T> castList(Class<T> type, List<?> list) {
        return list.stream().map(type::cast).collect(Collectors.toList());
    }
}
