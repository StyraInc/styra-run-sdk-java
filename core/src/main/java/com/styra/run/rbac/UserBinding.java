package com.styra.run.rbac;

import com.styra.run.SerializableAsMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UserBinding implements SerializableAsMap {
    private final User user;
    private final List<Role> roles;

    public UserBinding(User user, List<Role> roles) {
        this.user = user;
        this.roles = roles;
    }

    public User getUser() {
        return user;
    }

    public List<Role> getRoles() {
        return roles;
    }

    @Override
    public Map<String, ?> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("roles", roles.stream().map(Role::getName).collect(Collectors.toList()));
        return map;
    }
}
