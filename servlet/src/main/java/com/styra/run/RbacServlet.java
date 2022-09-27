package com.styra.run;

import com.styra.run.rbac.AuthorizationInput;
import com.styra.run.rbac.RbacManager;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutionException;

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
    }

    private final class RbacRolesServlet extends RbacSubRoute {
        private RbacRolesServlet(String path) {
            super(path);
        }

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            RbacManager rbac = getRbacManager();
            try {
                List<String> roles = rbac.getRoles(new AuthorizationInput("alice", "acmecorp")).get();

                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("application/json");
                response.getOutputStream().write(toJson(roles).getBytes(StandardCharsets.UTF_8));
            } catch (InterruptedException | ExecutionException e) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    private final class RbacUserBindingsServlet extends RbacSubRoute {
        private RbacUserBindingsServlet(String path) {
            super(path);
        }

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            response.setStatus(HttpServletResponse.SC_OK);
            response.getOutputStream().write("USER BINDINGS".getBytes(StandardCharsets.UTF_8));
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
            String userId = getPath(request);

            if (userId.indexOf('/') != -1 || userId.isEmpty()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            StyraRun styraRun = getStyraRun();

            response.setStatus(HttpServletResponse.SC_OK);
            response.getOutputStream().write(userId.getBytes(StandardCharsets.UTF_8));
        }
    }
}
