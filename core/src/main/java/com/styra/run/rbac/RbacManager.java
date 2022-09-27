package com.styra.run.rbac;

import com.styra.run.StyraRun;
import com.styra.run.StyraRunException;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class RbacManager {
    private static final String AUTHZ_PATH = "rbac/manage/allow";
    private static final String ROLES_PATH = "rbac/roles";

    private final StyraRun styraRun;

    public RbacManager(StyraRun styraRun) {
        this.styraRun = styraRun;
    }

    public CompletableFuture<List<String>> getRoles(AuthorizationInput authzInput) {
        return styraRun.check(AUTHZ_PATH, authzInput)
                .thenCompose((allowed) -> {
                    if (!allowed) {
                        throw new CompletionException(new StyraRunException("Not authorized to manage RBAC"));
                    }
                    return styraRun.query(ROLES_PATH)
                            .thenApply((result) -> result.asListOf(String.class));
                });
    }

    public CompletableFuture<List<UserBinding>> listUserBindings(AuthorizationInput authzInput) {
        return null;
    }

    public CompletableFuture<UserBinding> getUserBinding(User user, AuthorizationInput authzInput) {
        return null;
    }

    public CompletableFuture<Void> setUserBinding(User user, List<Role> roles, AuthorizationInput authzInput) {
        return null;
    }

    public CompletableFuture<Void> deleteUserBinding(User user, AuthorizationInput authzInput) {
        return null;
    }
}
