package com.styra.run.servlet.rbac;

import com.styra.run.rbac.User;
import com.styra.run.session.TenantSession;

import java.util.Collections;
import java.util.List;

public class ListUserProvider implements UserProvider {
    private final List<User> users;

    public ListUserProvider(List<User> users) {
        this.users = Collections.unmodifiableList(users);
    }

    @Override
    public PagedData<User> get(String page, TenantSession session) {
        return new PagedData<>(users, null);
    }
}
