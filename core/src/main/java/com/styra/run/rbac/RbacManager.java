package com.styra.run.rbac;

import com.styra.run.AuthorizationException;
import com.styra.run.StyraRun;
import com.styra.run.StyraRunException;
import com.styra.run.utils.Futures;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import static com.styra.run.utils.Futures.async;
import static com.styra.run.utils.Types.castList;
import static com.styra.run.utils.Url.joinPath;

public class RbacManager {
    public static final String AUTHZ_PATH = "rbac/manage/allow";
    private static final String ROLES_PATH = "rbac/roles";
    private static final String USER_BINDINGS_PATH = "rbac/user_bindings";

    private final StyraRun styraRun;

    public RbacManager(StyraRun styraRun) {
        this.styraRun = styraRun;
    }

    public CompletableFuture<List<String>> getRoles(AuthorizationInput authzInput) {
        return styraRun.check(AUTHZ_PATH, authzInput)
                .thenApply(this::assertAllowed)
                .thenCompose((Void) -> styraRun.query(ROLES_PATH))
                .thenApply(async((result) -> result.getListOf(String.class)));
    }

    public CompletableFuture<UserBinding> getUserBinding(User user, AuthorizationInput authzInput) {
        return styraRun.check(AUTHZ_PATH, authzInput)
                .thenApply(this::assertAllowed)
                .thenCompose((Void) -> getUserBinding(authzInput.getTenant(), user));
    }

    public CompletableFuture<Void> setUserBinding(User user, List<Role> roles, AuthorizationInput authzInput) {
        List<String> data = roles.stream().map(Role::getName).collect(Collectors.toList());
        return styraRun.check(AUTHZ_PATH, authzInput)
                .thenApply(this::assertAllowed)
                .thenCompose((Void) -> styraRun.putData(
                        joinPath(USER_BINDINGS_PATH, authzInput.getTenant(), user.getId()),
                        data))
                .thenApply((Void) -> null);
    }

    public CompletableFuture<Void> deleteUserBinding(User user, AuthorizationInput authzInput) {
        return styraRun.check(AUTHZ_PATH, authzInput)
                .thenApply(this::assertAllowed)
                .thenCompose((Void) -> styraRun.deleteData(
                        joinPath(USER_BINDINGS_PATH, authzInput.getTenant(), user.getId())))
                .thenApply((Void) -> null);
    }

    public CompletableFuture<List<UserBinding>> listUserBindings(AuthorizationInput authzInput) {
        return styraRun.check(AUTHZ_PATH, authzInput)
                .thenApply(this::assertAllowed)
                .thenCompose((Void) -> styraRun.getData(
                        joinPath(USER_BINDINGS_PATH, authzInput.getTenant())))
                .thenApply(async((result) ->
                        result.getMapOf(List.class).entrySet().stream()
                                .map((entry) -> {
                                    User user = new User(entry.getKey());
                                    List<Role> roles = castList(String.class, (List<?>) entry.getValue())
                                            .stream()
                                            .map(Role::new)
                                            .collect(Collectors.toList());
                                    return new UserBinding(user, roles);
                                })
                                .collect(Collectors.toList()), (e) -> new StyraRunException("Failed to parse user bindings", e)));
    }

    public CompletableFuture<List<UserBinding>> getUserBindings(List<User> users, AuthorizationInput authzInput) {
        return styraRun.check(AUTHZ_PATH, authzInput)
                .thenApply(this::assertAllowed)
                .thenCompose((Void) -> {
                    List<CompletableFuture<UserBinding>> futures = users.stream()
                            .map(user -> getUserBinding(authzInput.getTenant(), user))
                            .collect(Collectors.toList());
                    return Futures.allOf(futures);
                });
    }

    private CompletableFuture<UserBinding> getUserBinding(String tenant, User user) {
        return styraRun.getData(joinPath(USER_BINDINGS_PATH, tenant, user.getId()))
                .thenApply((result) -> {
                    try {
                        List<Role> roles = result.getListOf(String.class).stream()
                                .map(Role::new)
                                .collect(Collectors.toList());
                        return new UserBinding(user, roles);
                    } catch (Exception e) {
                        throw new CompletionException(
                                new StyraRunException(String.format("Failed to parse user binding for '%s'", user.getId()), e));
                    }
                });
    }


    private Void assertAllowed(boolean allowed) {
        if (!allowed) {
            throw new CompletionException(new AuthorizationException("Not authorized to manage RBAC"));
        }

        return null;
    }
}
