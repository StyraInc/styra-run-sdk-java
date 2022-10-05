package com.styra.run.discovery;

import com.styra.run.utils.Null;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class Gateway {
    private final URI uri;
    private final Map<String, ?> attributes;

    public Gateway(URI uri) {
        this(uri, Collections.emptyMap());
    }

    public Gateway(URI uri, Map<String, ?> attributes) {
        this.uri = uri;
        this.attributes = Null.map(attributes, Collections::unmodifiableMap, Collections.emptyMap());
    }

    public static Gateway fromResponseMap(Map<?, ?> map) throws URISyntaxException {
        return new Gateway(
                new URI(map.get("gateway_url").toString()),
                map.entrySet().stream()
                        .filter((e) -> !"gateway_url".equals(e.getKey()))
                        .collect(Collectors.toMap(
                                (entry) -> entry.getKey().toString(),
                                Map.Entry::getValue)));
    }

    public URI getUri() {
        return uri;
    }

    public Map<String, ?> getAttributes() {
        return attributes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Gateway gateway = (Gateway) o;
        return uri.equals(gateway.uri) && attributes.equals(gateway.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri);
    }

    @Override
    public String toString() {
        return "Gateway{" +
                "uri=" + uri +
                '}';
    }
}
