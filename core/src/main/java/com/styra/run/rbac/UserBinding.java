package com.styra.run.rbac;

import java.util.List;

public class UserBinding {
    private final User id;
    private final List<Role> roles;

    public UserBinding(User id, List<Role> roles) {
        this.id = id;
        this.roles = roles;
    }

    public User getId() {
        return id;
    }

    public List<Role> getRoles() {
        return roles;
    }
}
