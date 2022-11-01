package com.styra.run;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ListResult extends Result<List<Result<?>>> {
    public static ListResult EMPTY = new ListResult(Collections.emptyList());

    public ListResult(List<Result<?>> resultList) {
        super(resultList);
    }

    public ListResult(List<Result<?>> resultList, Map<String, ?> attributes) {
        super(resultList, attributes);
    }

    public static ListResult empty() {
        return EMPTY;
    }

    public static ListResult fromResponseMap(Map<?, ?> map) {
        List<?> items = (List<?>) map.get("result");
        if (items == null) {
            items = Collections.emptyList();
        }

        return new ListResult(
                items.stream()
                        .map(Map.class::cast)
                        .map(Result::fromResponseMap)
                        .collect(Collectors.toList()),
                map.entrySet().stream()
                        .filter((e) -> !"result".equals(e.getKey()))
                        .collect(Collectors.toMap(
                                (entry) -> entry.getKey().toString(),
                                Map.Entry::getValue)));
    }

    public ListResult withoutAttributes() {

        return new ListResult(get().stream()
                .map(Result::withoutAttributes)
                .collect(Collectors.toList()));
    }

    public int size() {
        return get().size();
    }

    public ListResult append(ListResult other) {
        List<Result<?>> items = Stream.concat(
                        get().stream(),
                        other.get().stream())
                .collect(Collectors.toList());
        Map<String, ?> attributes = Stream.concat(
                        getAttributes().entrySet().stream(),
                        other.getAttributes().entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return new ListResult(items, attributes);
    }
}
