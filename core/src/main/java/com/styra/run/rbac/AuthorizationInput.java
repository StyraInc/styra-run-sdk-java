package com.styra.run.rbac;

import com.styra.run.MapInput;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AuthorizationInput extends MapInput<String, Object> {
    private static final String KEY_SUBJECT = "subject";
    private static final String KEY_TENANT = "tenant";

    public AuthorizationInput(String subject, String tenant) {
        super(createMap(subject, tenant));

        Objects.requireNonNull(subject, "subject must not be null");
        Objects.requireNonNull(tenant, "tenant must not be null");
    }

    private AuthorizationInput(Map<String, Object> map) {
        super(map);

        if (!map.containsKey(KEY_SUBJECT)) {
            throw new IllegalArgumentException(String.format("map doesnt contain %s entry", KEY_SUBJECT));
        }

        if (!map.containsKey(KEY_TENANT)) {
            throw new IllegalArgumentException(String.format("map doesnt contain %s entry", KEY_TENANT));
        }
    }

    private static Map<String, Object> createMap(String subject, String tenant) {
        Map<String, Object> map = new HashMap<>(2);
        map.put(KEY_SUBJECT, subject);
        map.put(KEY_TENANT, tenant);
        return map;
    }

    public AuthorizationInput with(String key, Object value) {
        Map<String, Object> map = new HashMap<>(getValue());
        map.put(key, value);
        return new AuthorizationInput(map);
    }
}
