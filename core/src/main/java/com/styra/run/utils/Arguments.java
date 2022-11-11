package com.styra.run.utils;

import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;


public class Arguments {
    public static String requireNotEmpty(String value, String message) {
        return require(value, (v) -> v != null && !v.isEmpty(), message);
    }

    public static <T, C extends Collection<T>> C requireNotEmpty(C value, String message) {
        return require(value, (v) -> v != null && !v.isEmpty(), message);
    }

    public static <K, V, M extends Map<K, V>> M requireNotEmpty(M value, String message) {
        return require(value, (v) -> v != null && !v.isEmpty(), message);
    }

    public static <T> T require(T value, Predicate<T> predicate, String message) {
        return Objects.require(value, predicate, () -> new IllegalArgumentException(message));
    }
}
