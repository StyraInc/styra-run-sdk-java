package com.styra.run.rbac;

import com.styra.run.SerializableAsMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

public class UserBinding implements SerializableAsMap {
    private final User user;
    private final List<Role> roles;

    public UserBinding(User user) {
        this(user, Collections.emptyList());
    }

    public UserBinding(User user, List<Role> roles) {
        this.user = requireNonNull(user, "user must not be null");
        this.roles = unmodifiableList(requireNonNull(roles, "roles must not be null"));
    }

    public User getUser() {
        return user;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public UserBinding withRole(String role) {
        return withRole(new Role(role));
    }

    public UserBinding withRole(Role role) {
        List<Role> roles = new ArrayList<>(this.roles.size() + 1);
        roles.addAll(this.roles);
        roles.add(role);
        return new UserBinding(user, roles);
    }

    @Override
    public Map<String, ?> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("roles", roles.stream().map(Role::getName).collect(Collectors.toList()));
        return map;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        UserBinding that = (UserBinding) other;
        return user.equals(that.user) && roles.equals(that.roles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, roles);
    }
}
