package com.styra.run.rbac;

import java.util.Objects;

import static com.styra.run.utils.Arguments.requireNotEmpty;

public class User {
    private final String id;

    public User(String id) {
        this.id = requireNotEmpty(id, "id must not be null or empty");
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        User user = (User) other;
        return id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
