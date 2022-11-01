package com.styra.run;

import java.util.HashMap;
import java.util.Map;

public class InputContainer implements SerializableAsMap {
    private final Input<?> input;

    public InputContainer(Input<?> input) {
        this.input = input;
    }

    public Input<?> getInput() {
        return input;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        if (input != null && !input.isEmpty()) {
            map.put("input", input.getValue());
        }
        return map;
    }
}
