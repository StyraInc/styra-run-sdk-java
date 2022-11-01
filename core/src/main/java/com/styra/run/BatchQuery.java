package com.styra.run;

import com.styra.run.utils.Null;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.styra.run.utils.Null.orThrow;
import static java.util.Objects.requireNonNull;

public class BatchQuery implements SerializableAsMap {
    private final List<Item> items;
    private final Input<?> input;

    public BatchQuery(List<Item> items, Input<?> input) {
        requireNonNull(items, "items must not be null");

        this.items = items;
        this.input = input;
    }

    public static BatchQuery fromMap(Map<?, ?> map) {
        List<Item> items = ((List<?>) map.get("items")).stream()
                .map((item) -> Item.fromMap((Map<?, ?>) item))
                .collect(Collectors.toList());
        Input<?> input = Null.map(map.get("input"), Input::new);
        return new BatchQuery(items, input);
    }

    public Input<?> getInput() {
        return input;
    }

    public List<Item> getItems() {
        return items;
    }

    List<BatchQuery> chunk(int chunkSize) {
        return com.styra.run.utils.Collections.chunk(items, chunkSize).stream()
                .map((items) -> new BatchQuery(items, input))
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, ?> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("items", items.stream()
                .map(InputContainer::toMap)
                .collect(Collectors.toList()));
        if (input != null && !input.isEmpty()) {
            map.put("input", input.getValue());
        }
        return map;
    }

    public static class Item extends InputContainer {
        private final String path;

        public Item(String path) {
            this(path, null);
        }

        public Item(String path, Input<?> input) {
            super(input);
            this.path = orThrow(path, () -> new IllegalArgumentException("path must not be null"));
        }

        public static Item fromMap(Map<?, ?> map) {
            String path = (String) map.get("path");
            Input<?> input = Null.map(map.get("input"), Input::new);
            return new Item(path, input);
        }

        public String getPath() {
            return path;
        }

        @Override
        public Map<String, Object> toMap() {
            Map<String, Object> map = super.toMap();
            map.put("path", path);
            return map;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Query{")
                    .append("path='").append(path).append('\'');

            Object input = getInput();
            if (input != null) {
                sb.append(", input=")
                        .append(input);
            }

            sb.append('}');
            return sb.toString();
        }
    }
}
