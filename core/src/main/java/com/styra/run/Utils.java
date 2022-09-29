package com.styra.run;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
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

        public static URI appendPath(URI base, String... path) throws StyraRunException {
            String joinedPath = joinPath(base.getPath(), path);
            try {
                return new URI(base.getScheme(), base.getHost(), joinedPath, base.getFragment());
            } catch (URISyntaxException e) {
                throw new StyraRunException("Failed to construct API URI", e);
            }
        }
    }

    final class Null {
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

    final class Types {
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

    final class Lambdas {
        @FunctionalInterface
        interface CheckedBiConsumer<T, U, E extends Throwable> {
            void accept(T t, U u) throws E;
        }

        @FunctionalInterface
        interface CheckedConsumer<T, E extends Throwable> {
            void accept(T t) throws E;
        }
    }

    // TODO: Make streaming version?
    final class Collections {
        public static <T> List<List<T>> chunk(List<T> list, int chunkSize) {
            List<List<T>> chunks = new LinkedList<>();
            if (chunkSize == 0) {
                chunks.add(list);
            } else {
                final AtomicReference<List<T>> currentChunk = new AtomicReference<>(new LinkedList<>());
                list.forEach((item) -> {
                    List<T> chunk = currentChunk.get();
                    if (chunk.size() < chunkSize) {
                        chunk.add(item);
                    } else {
                        chunks.add(chunk);
                        currentChunk.set(new LinkedList<>());
                    }
                });
                if (!currentChunk.get().isEmpty()) {
                    chunks.add(currentChunk.get());
                }
            }
            return chunks;
        }
    }

    final class Futures {
        public static <T> CompletableFuture<List<T>> allOf(List<CompletableFuture<T>> futures) {
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply((Void) -> futures.stream()
                            .map(CompletableFuture::join)
                            .collect(Collectors.toList()));
        }
    }
}
