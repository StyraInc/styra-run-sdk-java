package com.styra.run.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class Lambdas {
    @FunctionalInterface
    public interface CheckedBiConsumer<T, U, E extends Throwable> {
        void accept(T t, U u) throws E;
    }

    @FunctionalInterface
    public interface CheckedConsumer<T, E extends Throwable> {
        void accept(T t) throws E;
    }

    @FunctionalInterface
    public interface CheckedSupplier<T, E extends Throwable> {
        T get() throws E;
    }

    @FunctionalInterface
    public interface CheckedFunction<T, R, E extends Throwable> {
        R apply(T val) throws E;
    }

    public static class CheckedValue<T, E extends Exception> {
        private final T value;
        private final E exception;

        public CheckedValue(T value, E exception) {
            this.value = value;
            this.exception = exception;
        }

        public static <T, R, E extends Exception> Function<T, CheckedValue<R, E>> tryWrap(CheckedFunction<T, R, E> supplier) {
            return t -> {
                R value;
                E exception;
                try {
                    value = supplier.apply(t);
                    exception = null;
                } catch (Exception e) {
                    value = null;
                    exception = (E) e;
                }

                return new CheckedValue<>(value, exception);
            };
        }

        public static <T, E extends Exception> List<T> unwrapOrThrow(List<CheckedValue<T, E>> list) throws E {
            List<T> resolved = new ArrayList<>(list.size());
            for (CheckedValue<T, E> t : list) {
                resolved.add(t.get());
            }
            return resolved;
        }

        public static <K, V, E extends Exception> Map<K, V> unwrapOrThrow(Map<K, CheckedValue<V, E>> map) throws E {
            Map<K, V> resolved = new HashMap<>(map.size());
            for (Map.Entry<K, CheckedValue<V, E>> entry : map.entrySet()) {
                resolved.put(entry.getKey(), entry.getValue().get());
            }
            return resolved;
        }

        public T get() throws E {
            if (exception != null) {
                throw exception;
            }
            return value;
        }
    }
}
