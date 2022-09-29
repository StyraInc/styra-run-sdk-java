package com.styra.run;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.styra.run.Utils.Null.firstNonNull;
import static java.util.Collections.singletonMap;

public class Result<T> implements SerializableAsMap {
    public static final Result<?> EMPTY_RESULT = new Result<>(null, Collections.emptyMap());

    private final T value;
    private final Map<String, ?> attributes;

    public Result(T value) {
        this(value, Collections.emptyMap());
    }

    public Result(T value, Map<String, ?> attributes) {
        this.value = value;
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
        return value;
    }

    public Optional<T> getOptional() {
        return Optional.ofNullable(value);
    }

    public boolean asBoolean() {
        return (Boolean) value;
    }

    public boolean asSafeBoolean(boolean def) {
        return value instanceof Boolean ? (Boolean) value : def;
    }

    public List<?> asList() {
        return firstNonNull((List<?>) value, Collections.emptyList());
    }

    public <U> List<U> asListOf(Class<U> type) {
        return asList().stream()
                .map(type::cast)
                .collect(Collectors.toList());
    }

    public Map<?, ?> asMap() {
        return firstNonNull((Map<?, ?>) value, Collections.emptyMap());
    }

    public List<Result<?>> asResultList() {
        return asList().stream()
                .map(Map.class::cast)
                .map(Result::fromResponseMap)
                .collect(Collectors.toList());
    }

    public Result<T> withoutAttributes() {
        return new Result<>(value, Collections.emptyMap());
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
        return Objects.equals(value, result1.value) && Objects.equals(attributes, result1.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, attributes);
    }

    @Override
    public String toString() {
        return "Result{" + value +
                ", attributes=" + attributes +
                '}';
    }

    public Map<String, ?> toMap() {
        if (value == null) {
            return Collections.emptyMap();
        }

        return singletonMap("result", SerializableAsMap.serialize(value));
    }
}
