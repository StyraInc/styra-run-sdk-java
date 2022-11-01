package com.styra.run.rbac;

import com.styra.run.AuthorizationException;
import com.styra.run.StyraRun;
import com.styra.run.StyraRunException;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import static com.styra.run.utils.Futures.async;
import static com.styra.run.utils.Types.castList;

public class RbacManager {
    public static final String AUTHZ_PATH = "rbac/manage/allow";
    private static final String ROLES_PATH = "rbac/roles";
    private static final String USER_BINDINGS_PATH = "rbac/user_bindings";
    private static final String USER_BINDINGS_FORMAT = USER_BINDINGS_PATH + "/%s";
    private static final String USER_BINDING_FORMAT = USER_BINDINGS_FORMAT + "/%s";

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

    public CompletableFuture<List<UserBinding>> listUserBindings(AuthorizationInput authzInput) {
        return styraRun.check(AUTHZ_PATH, authzInput)
                .thenApply(this::assertAllowed)
                .thenCompose((Void) -> styraRun.getData(String.format(USER_BINDINGS_FORMAT, authzInput.getTenant())))
                .thenApply(async((result) ->
                        result.getMapOf(List.class).entrySet().stream()
                                .map((entry) -> {
                                    User user = new User(entry.getKey().toString());
                                    List<Role> roles = castList(String.class, (List<?>) entry.getValue())
                                            .stream()
                                            .map(Role::new)
                                            .collect(Collectors.toList());
                                    return new UserBinding(user, roles);
                                })
                                .collect(Collectors.toList()), (e) -> new StyraRunException("Failed to parse user bindings", e)));
    }

    // TODO: Add getUserBindings(List<User>, AuthorizationInput) function

    public CompletableFuture<UserBinding> getUserBinding(User user, AuthorizationInput authzInput) {
        return styraRun.check(AUTHZ_PATH, authzInput)
                .thenApply(this::assertAllowed)
                .thenCompose((Void) -> styraRun.getData(String.format(USER_BINDING_FORMAT, authzInput.getTenant(), user.getId())))
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

    public CompletableFuture<Void> setUserBinding(User user, List<Role> roles, AuthorizationInput authzInput) {
        List<String> data = roles.stream().map(Role::getName).collect(Collectors.toList());
        return styraRun.check(AUTHZ_PATH, authzInput)
                .thenApply(this::assertAllowed)
                .thenCompose((Void) -> styraRun.putData(String.format(USER_BINDING_FORMAT, authzInput.getTenant(), user.getId()), data))
                .thenApply((Void) -> null);
    }

    public CompletableFuture<Void> deleteUserBinding(User user, AuthorizationInput authzInput) {
        return styraRun.check(AUTHZ_PATH, authzInput)
                .thenApply(this::assertAllowed)
                .thenCompose((Void) -> styraRun.deleteData(String.format(USER_BINDING_FORMAT, authzInput.getTenant(), user.getId())))
                .thenApply((Void) -> null);
    }

    private Void assertAllowed(boolean allowed) {
        if (!allowed) {
            throw new CompletionException(new AuthorizationException("Not authorized to manage RBAC"));
        }

        return null;
    }
}
