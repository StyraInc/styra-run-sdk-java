package com.styra.run.test;

import com.styra.run.servlet.ProxyServlet;
import com.styra.run.servlet.RbacServlet;
import com.styra.run.StyraRun;
import com.styra.run.servlet.session.CookieSessionManager;
import com.styra.run.session.TenantInputTransformer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class TestServer {
    public static void main(String... args) throws Exception {
        var url = "http://localhost:4000";
        var token = "foobar";

        var server = new Server(3000);
        var root = new ServletContextHandler();
        server.setHandler(root);

        var styraRun = StyraRun.builder(url, token).build();
        var sessionManager = new CookieSessionManager();
        var inputTransformer = new TenantInputTransformer();

        var proxyServlet = ProxyServlet.from(styraRun, sessionManager, inputTransformer);
        root.addServlet(new ServletHolder(proxyServlet), "/batch_query");

        var rbacServlet = RbacServlet.from(styraRun, sessionManager, inputTransformer);
        root.addServlet(new ServletHolder(rbacServlet), "/rbac/*");

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
