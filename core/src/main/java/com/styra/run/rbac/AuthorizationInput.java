package com.styra.run.rbac;

import com.styra.run.Input;
import com.styra.run.MapInput;
import com.styra.run.utils.Types;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.styra.run.utils.Null.firstNonNull;

public class AuthorizationInput extends MapInput<String, Object> {
    private static final String SUBJECT_KEY = "subject";
    private static final String TENANT_KEY = "tenant";

    public AuthorizationInput(String subject, String tenant) {
        super(createMap(subject, tenant));

        Objects.requireNonNull(subject, "subject must not be null");
        Objects.requireNonNull(tenant, "tenant must not be null");
    }

    private AuthorizationInput(Map<String, Object> map) {
        super(map);

        if (!map.containsKey(SUBJECT_KEY)) {
            throw new IllegalArgumentException(String.format("map doesnt contain %s entry", SUBJECT_KEY));
        }

        if (!map.containsKey(TENANT_KEY)) {
            throw new IllegalArgumentException(String.format("map doesnt contain %s entry", TENANT_KEY));
        }
    }

    public static AuthorizationInput from(Input<?> other) {
        Map<?, ?> value = Types.cast(Map.class, firstNonNull(other::getValue, Collections::emptyMap),
                () -> new IllegalArgumentException("other input value is not a Map"));

        return new AuthorizationInput(Types.castMap(Object.class, value));
    }

    private static Map<String, Object> createMap(String subject, String tenant) {
        Map<String, Object> map = new HashMap<>(2);
        map.put(SUBJECT_KEY, subject);
        map.put(TENANT_KEY, tenant);
        return map;
    }

    public AuthorizationInput with(String key, Object value) {
        Map<String, Object> map = new HashMap<>(getValue());
        map.put(key, value);
        return new AuthorizationInput(map);
    }

    public String getTenant() {
        return (String) getValue().get(TENANT_KEY);
    }
}
