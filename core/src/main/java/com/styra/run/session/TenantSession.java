package com.styra.run.session;

import com.styra.run.Input;
import com.styra.run.MapInput;
import com.styra.run.utils.Types;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.styra.run.utils.Arguments.require;
import static com.styra.run.utils.Arguments.requireNotEmpty;
import static com.styra.run.utils.Null.firstNonNull;

/**
 * A tenant-session containing a required tenant identifier, and an optional subject identifier.
 */
public class TenantSession extends MapInput<String, Object> implements Session {
    private static final String SUBJECT_KEY = "subject";
    private static final String TENANT_KEY = "tenant";

    public TenantSession(String subject, String tenant) {
        super(createMap(subject,
                requireNotEmpty(tenant, "tenant must not be null or empty")));
    }

    public TenantSession(Map<String, Object> map) {
        super(require(map, (m) -> m != null && m.containsKey(TENANT_KEY),
                String.format("map doesn't contain %s entry", TENANT_KEY)));
    }

    public static TenantSession from(Input<?> other) {
        Map<?, ?> value = Types.cast(Map.class, firstNonNull(other::getValue, Collections::emptyMap),
                () -> new IllegalArgumentException("other input value is not a Map"));

        return new TenantSession(Types.castMap(Object.class, value));
    }

    private static Map<String, Object> createMap(String subject, String tenant) {
        Map<String, Object> map = new HashMap<>(2);
        map.put(SUBJECT_KEY, subject);
        map.put(TENANT_KEY, tenant);
        return map;
    }

    public TenantSession with(String key, Object value) {
        Map<String, Object> map = new HashMap<>(getValue());
        map.put(key, value);
        return new TenantSession(map);
    }

    public String getTenant() {
        return (String) getValue().get(TENANT_KEY);
    }

    @Override
    public Input<?> toInput() {
        return this;
    }
}
