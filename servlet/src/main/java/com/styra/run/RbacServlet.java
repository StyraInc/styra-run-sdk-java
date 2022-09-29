package com.styra.run;

import com.styra.run.rbac.AuthorizationInput;
import com.styra.run.rbac.RbacManager;
import com.styra.run.rbac.Role;
import com.styra.run.rbac.User;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static com.styra.run.Utils.Null.firstNonNull;

public final class RbacServlet extends StyraRunServlet {
    private final RbacSubRoute[] subRoutes = new RbacSubRoute[]{
            new RbacRolesServlet("/roles"),
            new RbacUserBindingsServlet("/user_bindings"),
            new RbacUserBindingServlet("/user_bindings/"),
    };

    public RbacServlet() {
        super();
    }

    public RbacServlet(StyraRun styraRun) {
        super(styraRun);
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
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

        String toJson(Object value) throws ServletException, IOException {
            return getStyraRun().getJson().from(value);
        }

        String getPath(HttpServletRequest request) {
            return firstNonNull(request.getPathInfo(), "/")
                    .replaceFirst(pathPrefix, "");
        }

        AuthorizationInput getAuthzInput(HttpServletRequest request) throws AuthorizationException {
            try {
                return AuthorizationInput.from(getInputTransformer().transform(Input.empty(), RbacManager.AUTHZ_PATH, request));
            } catch (Throwable t) {
                throw new AuthorizationException("", t);
            }
        }
    }

    private final class RbacRolesServlet extends RbacSubRoute {
        private RbacRolesServlet(String path) {
            super(path);
        }

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            RbacManager rbac = getRbacManager();
            handleAsync(request, response, (body, out, async) ->
                    rbac.getRoles(getAuthzInput(request))
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
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            RbacManager rbac = getRbacManager();
            handleAsync(request, response, (body, out, async) ->
                    rbac.listUserBindings(getAuthzInput(request))
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
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            try {
                String userId = getUserId(request);
                RbacManager rbac = getRbacManager();
                handleAsync(request, response, (body, out, async) ->
                        rbac.getUserBinding(new User(userId), getAuthzInput(request))
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
        protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            try {
                String userId = getUserId(request);
                RbacManager rbac = getRbacManager();
                handleAsync(request, response, (body, out, async) ->
                        rbac.deleteUserBinding(new User(userId), getAuthzInput(request))
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
        protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            try {
                String userId = getUserId(request);
                RbacManager rbac = getRbacManager();
                handleAsync(request, response, (body, out, async) -> {
                    List<Role> roles = getStyraRun().getJson().toList(String.class, body)
                            .stream()
                            .map(Role::new)
                            .collect(Collectors.toList());
                    rbac.setUserBinding(new User(userId), roles, getAuthzInput(request))
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

        private String getUserId(HttpServletRequest request) throws NotFoundException {
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
