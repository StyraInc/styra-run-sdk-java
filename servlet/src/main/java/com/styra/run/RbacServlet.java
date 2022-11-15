package com.styra.run;

import com.styra.run.exceptions.AuthorizationException;
import com.styra.run.rbac.UserBinding;
import com.styra.run.session.SessionManager;
import com.styra.run.session.TenantSession;
import com.styra.run.rbac.RbacManager;
import com.styra.run.rbac.Role;
import com.styra.run.rbac.User;
import com.styra.run.session.InputTransformer;
import com.styra.run.session.Session;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static com.styra.run.utils.Null.firstNonNull;

/**
 * A servlet wrapping the functionality provided by {@link RbacManager}.
 *
 * @see StyraRunServlet
 */
public final class RbacServlet extends StyraRunServlet {
    private final RbacSubRoute[] subRoutes = new RbacSubRoute[]{
            new RbacRolesServlet("/roles"),
            new RbacUserBindingsServlet("/user_bindings"),
            new RbacUserBindingServlet("/user_bindings/"),
    };

    public RbacServlet() {
        super();
    }

    private RbacServlet(StyraRun styraRun,
                        SessionManager<Session> sessionManager,
                        InputTransformer<Session> inputTransformer) {
        super(styraRun, sessionManager, inputTransformer);
    }

    static <S extends Session> RbacServlet from(StyraRun styraRun,
                                                SessionManager<S> sessionManager,
                                                InputTransformer<S> inputTransformer) {
        //noinspection unchecked
        return new RbacServlet(styraRun,
                (SessionManager<Session>) sessionManager,
                (InputTransformer<Session>) inputTransformer);
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = firstNonNull(request.getPathInfo(), "/");

        for (RbacSubRoute subRoute : subRoutes) {
            if (subRoute.canService(path)) {
                subRoute.service(request, response);
                return;
            }
        }

        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private class RbacSubRoute extends HttpServlet {
        protected final String pathPrefix;

        private RbacSubRoute(String path) {
            this.pathPrefix = path;
        }

        boolean canService(String path) {
            return pathPrefix.equals(path);
        }

        StyraRun getStyraRun() throws ServletException {
            return RbacServlet.this.getStyraRun();
        }

        RbacManager getRbacManager() throws ServletException {
            return new RbacManager(getStyraRun());
        }

        String getPath(HttpServletRequest request) {
            return firstNonNull(request.getPathInfo(), "/")
                    .replaceFirst(pathPrefix, "");
        }

        TenantSession getSession(HttpServletRequest request)
                throws AuthorizationException {
            try {
                Session session = getSessionManager().getSession(request);
                if (session instanceof TenantSession) {
                    return (TenantSession) session;
                }
                return TenantSession.from(getInputTransformer()
                        .transform(Input.empty(), RbacManager.AUTHZ_PATH, session));
            } catch (Throwable t) {
                throw new AuthorizationException("Failed to form authorization input from session data", t);
            }
        }
    }

    private final class RbacRolesServlet extends RbacSubRoute {
        private RbacRolesServlet(String path) {
            super(path);
        }

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException {
            RbacManager rbac = getRbacManager();
            handleAsync(request, response, (body, out, async) ->
                    rbac.getRoles(getSession(request))
                            .thenAccept((roles) -> writeResult(roles, response, out, async))
                            .exceptionally((e) -> {
                                handleError("Failed to GET roles", e, async, response);
                                return null;
                            }));
        }
    }

    private final class RbacUserBindingsServlet extends RbacSubRoute {
        private RbacUserBindingsServlet(String path) {
            super(path);
        }

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException {
            RbacManager rbac = getRbacManager();
            handleAsync(request, response, (body, out, async) ->
                    rbac.listUserBindings(getSession(request))
                            .thenAccept((bindings) -> writeResult(bindings, response, out, async))
                            .exceptionally((e) -> {
                                handleError("Failed to GET user bindings for", e, async, response);
                                return null;
                            }));

        }
    }

    private final class RbacUserBindingServlet extends RbacSubRoute {
        private RbacUserBindingServlet(String path) {
            super(path);
        }

        @Override
        boolean canService(String path) {
            return path.startsWith(pathPrefix);
        }

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException {
            try {
                String userId = getUserId(request);
                RbacManager rbac = getRbacManager();
                handleAsync(request, response, (body, out, async) ->
                        rbac.getUserBinding(new User(userId), getSession(request))
                                .thenAccept((binding) -> writeResult(binding.getRoles().stream()
                                        .map(Role::getName)
                                        .collect(Collectors.toList()), response, out, async))
                                .exceptionally((e) -> {
                                    handleError(String.format("Failed to GET user binding for '%s'", userId), e, async, response);
                                    return null;
                                }));
            } catch (NotFoundException e) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        }

        @Override
        protected void doDelete(HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException {
            try {
                String userId = getUserId(request);
                RbacManager rbac = getRbacManager();
                handleAsync(request, response, (body, out, async) ->
                        rbac.deleteUserBinding(new User(userId), getSession(request))
                                .thenAccept((Void) -> writeResult(null, response, out, async))
                                .exceptionally((e) -> {
                                    handleError(String.format("Failed to PUT user binding for '%s'", userId), e, async, response);
                                    return null;
                                }));
            } catch (NotFoundException e) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        }

        @Override
        protected void doPut(HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException {
            try {
                String userId = getUserId(request);
                RbacManager rbac = getRbacManager();
                handleAsync(request, response, (body, out, async) -> {
                    User user = new User(userId);
                    List<Role> roles = getStyraRun().getJson().toList(String.class, body)
                            .stream()
                            .map(Role::new)
                            .collect(Collectors.toList());
                    UserBinding userBinding = new UserBinding(user, roles);
                    rbac.setUserBinding(userBinding, getSession(request))
                            .thenAccept((Void) -> writeResult(null, response, out, async))
                            .exceptionally((e) -> {
                                handleError(String.format("Failed to DELETE user binding for '%s'", userId), e, async, response);
                                return null;
                            });
                });
            } catch (NotFoundException e) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        }

        private String getUserId(HttpServletRequest request)
                throws NotFoundException {
            String userId = getPath(request);

            if (userId.indexOf('/') != -1 || userId.isEmpty()) {
                throw new NotFoundException();
            }

            return userId;
        }
    }

    private void writeResult(Object value, HttpServletResponse response, ServletOutputStream out, AsyncContext context) {
        writeOkJsonResponse(new Result<>(SerializableAsMap.serialize(value)).toMap(), response, out, context);
    }

    private static final class NotFoundException extends Exception {
    }
}
