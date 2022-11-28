package com.styra.run.servlet.rbac;

import com.styra.run.StyraRun;
import com.styra.run.servlet.session.SessionManager;
import com.styra.run.session.Session;
import com.styra.run.session.TenantSession;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import static com.styra.run.utils.Url.joinPath;

public final class RbacServletHelper {
    public static <S extends Session> void addRbacServlets(ServletContextHandler contextHandler,
                                                           String path) {
        addRbacServlets(contextHandler, path, null, null, null);
    }

    public static void addRbacServlets(ServletContextHandler contextHandler,
                                       String path,
                                       StyraRun styraRun,
                                       SessionManager<TenantSession> sessionManager,
                                       UserProvider userProvider) {
        contextHandler.addServlet(
                new ServletHolder(new RbacRolesServlet(styraRun, sessionManager)),
                joinPath(path, "roles"));

        contextHandler.addServlet(
                new ServletHolder(new RbacUserBindingsListServlet(styraRun, sessionManager, userProvider)),
                joinPath(path, "user_bindings"));

        contextHandler.addServlet(
                new ServletHolder(new RbacUserBindingServlet(styraRun, sessionManager)),
                joinPath(path, "user_bindings/*"));
    }
}
