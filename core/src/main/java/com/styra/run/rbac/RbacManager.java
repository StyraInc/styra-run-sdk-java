package com.styra.run.rbac;

import com.styra.run.exceptions.AuthorizationException;
import com.styra.run.StyraRun;
import com.styra.run.exceptions.StyraRunException;
import com.styra.run.session.TenantSession;
import com.styra.run.utils.Futures;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import static com.styra.run.utils.Futures.async;
import static com.styra.run.utils.Types.castList;
import static com.styra.run.utils.Url.joinPath;

/**
 * Manager of RBAC bindings maintained in Styra Run.
 */
public class RbacManager {
    public static final String AUTHZ_PATH = "rbac/manage/allow";
    private static final String ROLES_PATH = "rbac/roles";
    private static final String USER_BINDINGS_PATH = "rbac/user_bindings";

    private final StyraRun styraRun;

    public RbacManager(StyraRun styraRun) {
        this.styraRun = styraRun;
    }

    /**
     * Get all roles for the tenant in the provided {@link TenantSession session}.
     *
     * @param session the {@link TenantSession session} information authorizing the request for a given tenant
     * @return a {@link CompletableFuture} that completes to a list of roles
     */
    public CompletableFuture<List<String>> getRoles(TenantSession session) {
        return styraRun.check(AUTHZ_PATH, session)
                .thenApply(this::assertAllowed)
                .thenCompose((Void) -> styraRun.query(ROLES_PATH))
                .thenApply(async((result) -> result.getListOf(String.class)));
    }

    /**
     * Get the {@link UserBinding user-binding} for the provided {@link User}.
     *
     * @param user the {@link User} to get the {@link UserBinding} for
     * @param session the {@link TenantSession session} information authorizing the request for a given tenant
     * @return a {@link CompletableFuture} for the action
     */
    public CompletableFuture<UserBinding> getUserBinding(User user, TenantSession session) {
        return styraRun.check(AUTHZ_PATH, session)
                .thenApply(this::assertAllowed)
                .thenCompose((Void) -> getUserBinding(session.getTenant(), user));
    }

    /**
     * Set the {@link UserBinding user-binding} for a {@link User}.
     *
     * @param userBinding the {@link UserBinding} to set
     * @param session the {@link TenantSession session} information authorizing the request for a given tenant
     * @return a {@link CompletableFuture} for the action
     */
    public CompletableFuture<Void> putUserBinding(UserBinding userBinding, TenantSession session) {
        List<String> data = userBinding.getRoles().stream().map(Role::getName).collect(Collectors.toList());
        return styraRun.check(AUTHZ_PATH, session)
                .thenApply(this::assertAllowed)
                .thenCompose((Void) -> styraRun.putData(
                        joinPath(USER_BINDINGS_PATH, session.getTenant(), userBinding.getUser().getId()),
                        data))
                .thenApply((Void) -> null);
    }

    /**
     * Set the user-binding for a {@link User}.
     *
     * @param user the {@link User} to delete the user-binding for
     * @param session the {@link TenantSession session} information authorizing the request for a given tenant
     * @return a {@link CompletableFuture} for the action
     */
    public CompletableFuture<Void> deleteUserBinding(User user, TenantSession session) {
        return styraRun.check(AUTHZ_PATH, session)
                .thenApply(this::assertAllowed)
                .thenCompose((Void) -> styraRun.deleteData(
                        joinPath(USER_BINDINGS_PATH, session.getTenant(), user.getId())))
                .thenApply((Void) -> null);
    }

    /**
     * Lists all {@link UserBinding user-bindings} for the tenant in the provided {@link TenantSession session}.
     * <p>
     * The returned list of user-bindings is sorted lexicographically by {@link User#getId() id}.
     *
     * @param session the {@link TenantSession session} information authorizing the request for a given tenant
     * @return a {@link CompletableFuture} that completes to a list of all {@link UserBinding user-bindings} for the tenant
     */
    public CompletableFuture<List<UserBinding>> listUserBindings(TenantSession session) {
        return styraRun.check(AUTHZ_PATH, session)
                .thenApply(this::assertAllowed)
                .thenCompose((Void) -> styraRun.getData(
                        joinPath(USER_BINDINGS_PATH, session.getTenant())))
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
                                .sorted(Comparator.comparing(entry -> entry.getUser().getId()))
                                .collect(Collectors.toList()), (e) -> new StyraRunException("Failed to parse user bindings", e)));
    }

    /**
     * Get the {@link UserBinding user-binding} for each {@link User} in <code>users</code>.
     * <p>
     * The returned list of bindings will retain the order of <code>users</code>.
     * <p>
     * If a user doesn't have associated bindings, the returned {@link UserBinding} entry
     * will contain an empty list of roles.
     *
     * @param users   the {@link User users} to retrieve {@link UserBinding user-bindings} for
     * @param session the {@link TenantSession session} information authorizing the request for a given tenant
     * @return a {@link CompletableFuture} that completes to a list of resolved {@link UserBinding user-bindings}
     */
    // FIXME: Should we allow request failures for individual user?
    // TODO: Do batch data-requests when supported by back-end
    public CompletableFuture<List<UserBinding>> getUserBindings(List<User> users, TenantSession session) {
        return styraRun.check(AUTHZ_PATH, session)
                .thenApply(this::assertAllowed)
                .thenCompose((Void) -> {
                    List<CompletableFuture<UserBinding>> futures = users.stream()
                            .map(user -> getUserBinding(session.getTenant(), user))
                            .collect(Collectors.toList());
                    return Futures.allOf(futures);
                });
    }

    private CompletableFuture<UserBinding> getUserBinding(String tenant, User user) {
        return styraRun.getData(joinPath(USER_BINDINGS_PATH, tenant, user.getId()))
                .thenApply(Futures.async((result) -> {
                    try {
                        List<Role> roles;
                        if (result.hasValue()) {
                            roles = result.getListOf(String.class).stream()
                                    .map(Role::new)
                                    .collect(Collectors.toList());
                        } else {
                            roles = Collections.emptyList();
                        }
                        return new UserBinding(user, roles);
                    } catch (Exception e) {
                        throw new StyraRunException(String.format("Failed to parse user binding for '%s'", user.getId()), e);
                    }
                }));
    }


    private Void assertAllowed(boolean allowed) {
        if (!allowed) {
            throw new CompletionException(new AuthorizationException("Not authorized to manage RBAC"));
        }

        return null;
    }
}
