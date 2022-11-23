package com.styra.run.rbac;

import java.util.Objects;

public class Role {
    private final String name;

    public Role(String name) {
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return name.equals(role.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
