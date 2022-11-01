package com.styra.run;

import com.styra.run.utils.Null;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MapInput<K, V> extends Input<Map<K, V>> {
    public MapInput(Map<K, V> value) {
        super(Collections.unmodifiableMap(value));
    }

    public static MapInput<?, ?> empty() {
        return new MapInput<>(Collections.emptyMap());
    }

    public static <T, U> MapInput<T, U> of(T key, U value) {
        return new MapInput<>(Collections.singletonMap(key, value));
    }

    public static MapInput<?, ?> from(Input<?> other) {
        Map<?, ?> value = (Map<?, ?>) other.getValue();
        return new MapInput<>(value);
    }

    public <T extends K, U extends V> MapInput<K, V> with(T key, U value) {
        Map<K, V> map = new HashMap<>(getValue());
        map.put(key, value);
        return new MapInput<>(map);
    }

    public boolean isEmpty() {
        return Null.map(getValue(), Map::isEmpty, true);
    }

    public static final class Builder<K, V> {
        private final Map<K, V> map = new HashMap<>();

        public Builder<K, V> put(K key, V value) {
            map.put(key, value);
            return this;
        }

        public Builder<K, V> putAll(Map<K, V> other) {
            map.putAll(other);
            return this;
        }

        public Builder<K, V> putFrom(MapInput<K, V> other) {
            map.putAll(other.getValue());
            return this;
        }

        public MapInput<K, V> build() {
            return new MapInput<>(new HashMap<>(map));
        }
    }
}
