package com.styra.run;

import java.util.Collections;
import java.util.Map;

public class Input<T> {
    private final T value;

    public Input(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    public Map<String, ?> toMap() {
        return Collections.singletonMap("input", value);
    }

    public boolean isEmpty() {
        return value == null;
    }

    @Override
    public String toString() {
        return "Input{" +
                "value=" + value +
                '}';
    }
}
