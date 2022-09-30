package com.styra.run;

import java.util.Collections;
import java.util.Map;

public class Input<T> implements SerializableAsMap {
    private static final Input<Void> EMPTY = new Input<>(null);
    private final T value;

    public Input(T value) {
        this.value = value;
    }

    public static Input<?> empty() {
        return EMPTY;
    }

    public T getValue() {
        return value;
    }

    public boolean isEmpty() {
        return value == null;
    }

    @Override
    public Map<String, ?> toMap() {
        return Collections.singletonMap("input", value);
    }

    @Override
    public String toString() {
        return "Input{" +
                "value=" + value +
                '}';
    }
}
