package com.styra.run;

import com.styra.run.Utils.Null;

import java.util.*;
import java.util.stream.Collectors;

import static com.styra.run.Utils.Null.safeCast;
import static java.util.Collections.singletonMap;

public class Result<T> implements SerializableAsMap {
    public static final Result<?> EMPTY_RESULT = new Result<>(null, Collections.emptyMap());

    private final T result;
    private final Map<String, ?> attributes;

    public Result(T result) {
        this(result, Collections.emptyMap());
    }

    public Result(T result, Map<String, ?> attributes) {
        this.result = result;
        this.attributes = Utils.Null.map(attributes, Collections::unmodifiableMap, Collections.emptyMap());
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

    public <U> List<U> asListOf(Class<U> type) {
        return asList().stream()
                .map(type::cast)
                .collect(Collectors.toList());
    }

    public List<Result<?>> asResultList() {
        return asList().stream()
                .map(Map.class::cast)
                .map(Result::fromResponseMap)
                .collect(Collectors.toList());
    }

    public Result<T> withoutAttributes() {
        return new Result<>(result, Collections.emptyMap());
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

    public Map<String, ?> toMap() {
        if (result == null) {
            return Collections.emptyMap();
        }

        Object transformedResult;
        if (result instanceof SerializableAsMap) {
            transformedResult = ((SerializableAsMap) result).toMap();
        } else if (result instanceof List) {
            transformedResult = ((List<?>) result).stream()
                    .map((r) -> Null.map(safeCast(SerializableAsMap.class, r),
                            SerializableAsMap::toMap, r))
                    .collect(Collectors.toList());
        } else {
            transformedResult = result;
        }

        return singletonMap("result", transformedResult);
    }
}
