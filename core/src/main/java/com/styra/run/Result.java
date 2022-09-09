package com.styra.run;

import java.util.*;
import java.util.stream.Collectors;

public class Result<T> {
    public static final Result<?> EMPTY_RESULT = new Result<>(null, Collections.emptyMap());

    private final T result;
    private final Map<String, ?> attributes;

    public Result(T result) {
        this(result, Collections.emptyMap());
    }

    public Result(T result, Map<String, ?> attributes) {
        this.result = result;
        this.attributes = Collections.unmodifiableMap(attributes);
    }

    public static Result<?> empty() {
        return EMPTY_RESULT;
    }

    public static Result<Void> empty(Map<String, ?> attributes) {
        return new Result<>(null, attributes);
    }

    public static Result<?> fromResponseMap(Map<?, ?> map) {
        return new Result<Object>(
                map.get("result"),
                map.entrySet().stream()
                        .filter((e) -> !"result".equals(e.getKey()))
                        .collect(Collectors.toMap(
                                (entry) -> entry.getKey().toString(),
                                Map.Entry::getValue)));
    }

    public T get() {
        return result;
    }

    public Optional<T> getOptional() {
        return Optional.ofNullable(result);
    }

    public boolean asBoolean() {
        return (Boolean) result;
    }

    public boolean asSafeBoolean(boolean def) {
        return result instanceof Boolean ? (Boolean) result : def;
    }

    public List<?> asList() {
        return (List<?>) result;
    }

    public List<Result<?>> asResultList() {
        return asList().stream()
                .map(Map.class::cast)
                .map(Result::fromResponseMap)
                .collect(Collectors.toList());
    }

    public Map<String, ?> getAttributes() {
        return attributes;
    }

    public T getAttribute(String key, Class<T> type) {
        return type.cast(attributes.get(key));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Result<?> result1 = (Result<?>) o;
        return Objects.equals(result, result1.result) && Objects.equals(attributes, result1.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(result, attributes);
    }

    @Override
    public String toString() {
        return "Result{" + result +
                ", attributes=" + attributes +
                '}';
    }
}
