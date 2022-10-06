package com.styra.run.utils;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class Null {
    @SafeVarargs
    public static <T> T firstNonNull(T... values) {
        return Stream.of(values)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    @SafeVarargs
    public static <T> T firstNonNull(Supplier<T>... suppliers) {
        return Stream.of(suppliers)
                .map(Supplier::get)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    public static <T> T orElse(T value, T def) {
        return value != null ? value : def;
    }

    public static <T> T orElse(T value, Supplier<T> def) {
        return value != null ? value : def.get();
    }

    public static <T, R> R map(T value, Function<T, R> action) {
        return map(value, action, (R) null);
    }

    public static <T, R> R map(T value, Function<T, R> action, R def) {
        return value != null ? action.apply(value) : def;
    }

    public static <T, R> R map(T value, Function<T, R> action, Supplier<R> def) {
        return value != null ? action.apply(value) : def.get();
    }

    public static <T> void ifNotNull(T value, Consumer<T> consumer) {
        if (value != null) {
            consumer.accept(value);
        }
    }

    public static <T, E extends Exception> T orThrow(T value, Supplier<E> exceptionSupplier) throws E {
        if (value != null) {
            return value;
        }
        throw exceptionSupplier.get();
    }
}
