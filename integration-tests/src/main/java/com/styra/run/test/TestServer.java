package com.styra.run.test;

import com.styra.run.StyraRun;
import com.styra.run.rbac.User;
import com.styra.run.servlet.ProxyServlet;
import com.styra.run.servlet.rbac.RbacRolesServlet;
import com.styra.run.servlet.rbac.RbacUserBindingServlet;
import com.styra.run.servlet.rbac.RbacUserBindingsListServlet;
import com.styra.run.servlet.rbac.UserProvider;
import com.styra.run.servlet.session.CookieTenantSessionManager;
import com.styra.run.session.TenantInputTransformer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.util.List;
import java.util.Map;

public class TestServer {
    public static void main(String... args) throws Exception {
        var url = "http://localhost:4000";
        var token = "foobar";

        var server = new Server(3000);
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
                )), 2);
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

        server.start();
        server.join();
    }
}
