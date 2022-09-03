package com.styra.run;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Utils {
    final class Url {
        public static String joinPath(String root, String... path) {
            return Stream.concat(
                            Stream.of(root.split("/")),
                            Stream.of(path)
                                    .filter(Objects::nonNull)
                                    .flatMap((elem) -> Stream.of(elem.split("/"))))
                    .map(String::trim)
                    .filter((elem) -> !elem.isEmpty())
                    .collect(Collectors.joining("/", "/", ""));
        }

        public static URL appendPath(URL base, String... path) throws StyraRunException {
            String joinedPath = joinPath(base.getPath(), path);
            try {
                return new URL(base.getProtocol(), base.getHost(), base.getPort(), joinedPath);
            } catch (MalformedURLException e) {
                throw new StyraRunException("Failed to construct API URI", e);
            }
        }
    }

    final class Nullable {
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
    }
}
