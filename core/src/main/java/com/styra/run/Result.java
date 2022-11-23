package com.styra.run;

import com.styra.run.exceptions.StyraRunException;
import com.styra.run.utils.Null;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.styra.run.utils.Lambdas.CheckedValue.tryWrap;
import static com.styra.run.utils.Lambdas.CheckedValue.unwrapOrThrow;
import static java.util.Collections.singletonMap;

/**
 * A <code>Result</code> as returned from calls to the Styra Run API.
 *
 * @param <T> the type of the Result's value
 */
public class Result<T> implements SerializableAsMap {
    public static final Result<?> EMPTY_RESULT = new Result<>(null, Collections.emptyMap());

    private final T value;
    private final Map<String, ?> attributes;

    public Result(T value) {
        this(value, Collections.emptyMap());
    }

    public Result(T value, Map<String, ?> attributes) {
        this.value = value;
        this.attributes = Null.map(attributes, Collections::unmodifiableMap, Collections.emptyMap());
    }

    /**
     * Returns an empty {@link Result} with no value.
     *
     * @return an empty {@link Result}
     */
    public static Result<?> empty() {
        return EMPTY_RESULT;
    }

    /**
     * Returns an empty {@link Result} with no value, but with a collection of associated attributes.
     *
     * @param attributes a collection of attributes to associate with the Result
     * @return an empty {@link Result} with attributes
     */
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

    /**
     * Returns the value of this result.
     *
     * @return the value of this result, or <code>null</code>
     */
    public T get() {
        return value;
    }

    /**
     * Returns the typed value of this result.
     * If the actual value is <code>null</code>, or not of a type assignable from <code>type</code>,
     * a {@link StyraRunException is thrown}.
     *
     * @param type the required type of the value
     * @param <R>  the required type of the result value
     * @return the value of this result
     * @throws StyraRunException if this result's value is not of the required type
     */
    public <R> R get(Class<R> type) throws StyraRunException {
        if (value == null) {
            throw new StyraRunException("Result value is null");
        }

        try {
            return type.cast(value);
        } catch (ClassCastException e) {
            throw new StyraRunException(String.format("Result value was expected to be of type %s, but was %s",
                    type.getCanonicalName(), value.getClass().getCanonicalName()), e);
        }
    }

    /**
     * Returns the typed value of this result.
     * If the value is <code>null</code>, or not of a type assignable from <code>type</code>,
     * the default value <code>def</code> is returned instead.
     *
     * @param type the required type of the value
     * @param def  the default value
     * @param <R>  the required type of the result value
     * @return the value of this result
     */
    public <R> R getSafe(Class<R> type, R def) {
        if (value == null) {
            return def;
        }

        try {
            return type.cast(value);
        } catch (ClassCastException e) {
            return def;
        }
    }

    /**
     * Returns <code>true</code> if this Result has a non-null value.
     *
     * @return <code>true</code> if this Result's value is not null
     */
    public boolean hasValue() {
        return value != null;
    }

    /**
     * Returns <code>true</code> if this Result's value is <code>null</code> or an empty {@link Collection},
     * <code>false</code> otherwise.
     *
     * @return <code>true</code> if this Result's value is <code>null</code> or an empty {@link Collection}
     */
    public boolean isEmpty() {
        if (value instanceof Collection) {
            return ((Collection<?>) value).isEmpty();
        } else if (value instanceof Map) {
            return ((Map<?, ?>) value).isEmpty();
        }
        return value == null;
    }

    /**
     * Returns a list where each item has a type assignable from <code>type</code>.
     * Throws a {@link StyraRunException}, if the value of this <code>Result</code> is <code>null</code>,
     * is not a {@link List}, or one of the list's items isn't a type assignable from <code>type</code>.
     *
     * @param type the required type of all list items
     * @param <V>  the required type of each list item
     * @return the value of this Result as a list of <code>type</code>s
     * @throws StyraRunException if this Result's value isn't a list of <code>type</code>s
     */
    public <V> List<V> getListOf(Class<V> type) throws StyraRunException {
        List<?> list = get(List.class);
        return unwrapOrThrow(list.stream()
                .map(tryWrap(item -> {
                    try {
                        return type.cast(item);
                    } catch (ClassCastException e) {
                        throw new StyraRunException(String.format("Invalid result list item type; expected %s, but was %s",
                                type.getCanonicalName(), item.getClass().getCanonicalName()), e);
                    }
                }))
                .collect(Collectors.toList()));
    }

    /**
     * Returns a map where each entry has a <code>String</code> key and a value with a type assignable from <code>type</code>.
     * Throws a {@link StyraRunException}, if the value of this <code>Result</code> is <code>null</code>,
     * is not a {@link Map}, or one of the map's entry values isn't a type assignable from <code>type</code>.
     *
     * @param type the required type of all map entry values
     * @param <V>  the required type of each map entry value
     * @return the value of this Result as a map of <code>String</code> to <code>type</code> entries
     * @throws StyraRunException if this Result's value isn't a map of <code>String</code> to <code>type</code> entries
     */
    public <V> Map<String, ?> getMapOf(Class<V> type) throws StyraRunException {
        Map<?, ?> map = get(Map.class);
        return unwrapOrThrow(map.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().toString(),
                        tryWrap(entry -> {
                            try {
                                return type.cast(entry.getValue());
                            } catch (ClassCastException e) {
                                throw new StyraRunException(String.format("Invalid result map entry value type; expected %s, but was %s",
                                        type.getCanonicalName(), entry.getValue().getClass().getCanonicalName()), e);
                            }
                        }))));
    }

    boolean isValueType(Class<?> type) {
        return type.isInstance(value);
    }

    boolean isBooleanValue() {
        return isValueType(Boolean.class);
    }

    public Map<String, ?> getAttributes() {
        return attributes;
    }

    public boolean containsAttribute(String key) {
        return attributes.containsKey(key);
    }

    public T getAttribute(String key, Class<T> type) throws StyraRunException {
        Object attribute = attributes.get(key);
        if (attribute == null) {
            throw new StyraRunException(String.format("Result attribute '%s' is null", key));
        }

        try {
            return type.cast(attribute);
        } catch (ClassCastException e) {
            throw new StyraRunException(String.format("Result attribute '%s' was expected to be of type %s, but was %s",
                    key, type.getCanonicalName(), attribute.getClass().getCanonicalName()), e);
        }
    }

    public Result<T> withoutAttributes() {
        return new Result<>(value, Collections.emptyMap());
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
