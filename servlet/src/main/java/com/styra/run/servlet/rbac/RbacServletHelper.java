package com.styra.run.servlet.rbac;

import com.styra.run.StyraRun;
import com.styra.run.servlet.session.SessionManager;
import com.styra.run.session.InputTransformer;
import com.styra.run.session.Session;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import static com.styra.run.utils.Url.joinPath;

public final class RbacServletHelper {
    public static <S extends Session> void addRbacServlets(ServletContextHandler contextHandler,
                                                           String path) {
        addRbacServlets(contextHandler, path, null, null, null, null);
    }

    public static <S extends Session> void addRbacServlets(ServletContextHandler contextHandler,
                                                           String path,
                                                           StyraRun styraRun,
                                                           SessionManager<S> sessionManager,
                                                           InputTransformer<S> inputTransformer,
                                                           UserProvider userProvider) {
        contextHandler.addServlet(
                new ServletHolder(RbacRolesServlet.from(styraRun, sessionManager, inputTransformer)),
                joinPath(path, "roles"));

        contextHandler.addServlet(
                new ServletHolder(RbacUserBindingsServlet.from(styraRun, sessionManager, inputTransformer, userProvider)),
                joinPath(path, "user_bindings"));

        contextHandler.addServlet(
                new ServletHolder(RbacUserBindingServlet.from(styraRun, sessionManager, inputTransformer)),
                joinPath(path, "user_bindings/*"));
    }
}
