package com.styra.run.servlet.pagination;

import com.styra.run.SerializableAsMap;

import java.util.HashMap;
import java.util.Map;

public class IndexedPage implements Page, SerializableAsMap {
    private final int index;
    private final int total;

    public IndexedPage(int index, int total) {
        this.index = index;
        this.total = total;
    }

    @Override
    public Map<String, ?> toMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("index", index);
        map.put("of", total);
        return map;
    }

    @Override
    public Object serialize() {
        return toMap();
    }
}
