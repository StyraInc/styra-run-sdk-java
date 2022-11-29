package com.styra.run.servlet.rbac;

import com.styra.run.rbac.User;
import com.styra.run.session.TenantSession;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;

/**
 * A convenience {@link UserProvider} that maps lists of {@link User users} to tenant String identifiers.
 */
public class UnpagedUserProvider implements UserProvider {
    private final Map<String, List<User>> usersByTenant;

    public UnpagedUserProvider(Map<String, List<User>> usersByTenant) {
        this.usersByTenant = Collections.unmodifiableMap(usersByTenant);
    }

    @Override
    public PagedData<User> get(String page, TenantSession session) {
        return new PagedData<>(usersByTenant.getOrDefault(session.getTenant(), emptyList()), null);
    }
}
