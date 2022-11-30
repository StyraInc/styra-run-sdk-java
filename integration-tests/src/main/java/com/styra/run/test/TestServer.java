package com.styra.run.test;

import com.styra.run.StyraRun;
import com.styra.run.rbac.User;
import com.styra.run.servlet.ProxyServlet;
import com.styra.run.servlet.rbac.RbacRolesServlet;
import com.styra.run.servlet.rbac.RbacUserBindingServlet;
import com.styra.run.servlet.rbac.RbacUserBindingsListServlet;
import com.styra.run.servlet.rbac.UserProvider;
import com.styra.run.session.TenantInputTransformer;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TestServer {
    private final Server server;

    public TestServer(int port) {
        this.server = new Server(port);
    }

    public void start() throws Exception {
        var url = "http://localhost:4000";
        var token = "foobar";

        var root = new ServletContextHandler();
        server.setHandler(root);

        var styraRun = StyraRun.builder(url, token).build();
        var sessionManager = new CookieTenantSessionManager();
        var inputTransformer = new TenantInputTransformer();

//        RbacServletHelper.addRbacServlets(root, "/rbac", styraRun, sessionManager, inputTransformer);

        var rbacRolesServlet = new RbacRolesServlet(styraRun, sessionManager);
        root.addServlet(new ServletHolder(rbacRolesServlet), "/roles");

        var rbacAllUserBindingsServlet = new RbacUserBindingsListServlet(styraRun, sessionManager, null);
        root.addServlet(new ServletHolder(rbacAllUserBindingsServlet), "/user_bindings_all");

        var userProvider = UserProvider.from(Map.of(
                "acmecorp", List.of(
                        new User("alice"),
                        new User("bob"),
                        new User("bryan"),
                        new User("emily"),
                        new User("harold"),
                        new User("vivian")
                )), 3);
        var rbacPagedUserBindingsServlet = new RbacUserBindingsListServlet(styraRun, sessionManager, userProvider);
        root.addServlet(new ServletHolder(rbacPagedUserBindingsServlet), "/user_bindings");

        var rbacUserBindingServlet = new RbacUserBindingServlet(styraRun, sessionManager);
        root.addServlet(new ServletHolder(rbacUserBindingServlet), "/user_bindings/*");

        var proxyServlet = new ProxyServlet<>(styraRun, sessionManager, inputTransformer);
        root.addServlet(new ServletHolder(proxyServlet), "/batch_query");

        var queryServlet = new QueryProxyServlet(styraRun);
        root.addServlet(new ServletHolder(queryServlet), "/query/*");

        var checkServlet = new CheckProxyServlet(styraRun);
        root.addServlet(new ServletHolder(checkServlet), "/check/*");

        var dataServlet = new DataProxyServlet(styraRun);
        root.addServlet(new ServletHolder(dataServlet), "/data/*");

        root.addServlet(StatusServlet.class, "/ready");
        root.addServlet(new ServletHolder(new KillServlet(server)), "/stop");

        server.start();
        System.err.println("Server started");
    }

    public void join() {
        try {
            server.join();
        } catch (InterruptedException e) {
            System.err.println("Server interrupted");
        }
    }

    public void stop() throws Exception {
        server.stop();
        System.err.println("Server stopped");
    }

    public static void main(String... args) throws Exception {
        var testServer = new TestServer(3000);
        testServer.start();
        testServer.join();
        System.exit(0);
    }

    public static class KillServlet extends HttpServlet {
        private final Server server;
        private final ScheduledExecutorService killExecutor = Executors.newSingleThreadScheduledExecutor();

        public KillServlet(Server server) {
            this.server = server;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
            try {
                resp.setStatus(200);
                resp.flushBuffer();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.err.println("Scheduled to stop");
            killExecutor.schedule(() -> {
                try {
                    System.err.println("Stopping");
                    server.stop();
                    synchronized (server) {
                        server.notify();
                    }
                } catch (Exception e) {
                    System.err.println("Failed to stop; terminating");
                    System.exit(1);
                }
            }, 2, TimeUnit.SECONDS);
        }
    }

    public static class StatusServlet extends HttpServlet {
        @Override
        protected void doHead(HttpServletRequest req, HttpServletResponse resp) {
            doGet(req, resp);
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
            resp.setStatus(200);
        }
    }
}
