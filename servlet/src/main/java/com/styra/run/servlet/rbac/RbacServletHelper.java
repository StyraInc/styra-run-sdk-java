package com.styra.run.servlet.rbac;

import com.styra.run.StyraRun;
import com.styra.run.rbac.User;
import com.styra.run.servlet.session.SessionManager;
import com.styra.run.session.Session;
import com.styra.run.session.TenantSession;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import static com.styra.run.utils.Url.joinPath;

/**
 * A helper for setting up servlets for RBAC management.
 */
public final class RbacServletHelper {
    /**
     * The same as {@link #addRbacServlets(ServletContextHandler, String, StyraRun, SessionManager, UserProvider)},
     * but the {@link StyraRun}-, {@link SessionManager}-, and {@link UserProvider} services are not directly specified.
     * Instead, these will be pulled from attributes on the servlet context; or defaults will be used, where applicable.
     *
     * @param contextHandler the context to add the RBAC servlets to
     * @param path the URL path under which to add the RBAC servlets
     * @param <S> the {@link Session} type
     * @see #addRbacServlets(ServletContextHandler, String, StyraRun, SessionManager, UserProvider)
     */
    public static <S extends Session> void addRbacServlets(ServletContextHandler contextHandler,
                                                           String path) {
        addRbacServlets(contextHandler, path, null, null, null);
    }

    /**
     * The same as calling {@link #addRbacServlets(ServletContextHandler, String, StyraRun, SessionManager, UserProvider)}
     * with <code>userProvider</code> set to <code>null</code>.
     *
     * @param contextHandler the context to add the RBAC servlets to
     * @param path the URL path under which to add the RBAC servlets
     * @param styraRun the {@link StyraRun} instance to use for communicating with the Styra Run API
     * @param sessionManager the {@link SessionManager session-manager} to use for composing session information. May be <code>null</code>.
     * @see #addRbacServlets(ServletContextHandler, String, StyraRun, SessionManager, UserProvider)
     */
    public static void addRbacServlets(ServletContextHandler contextHandler,
                                       String path,
                                       StyraRun styraRun,
                                       SessionManager<TenantSession> sessionManager) {
        addRbacServlets(contextHandler, path, styraRun, sessionManager, null);
    }

    /**
     * Sets upp the following RBAC management servlets:
     * <ul>
     *     <li>{@link RbacRolesServlet}, for listing RBAC roles</li>
     *     <li>{@link RbacUserBindingServlet}, for reading, updating, and deleting a single RBAC user-binding</li>
     *     <li>{@link RbacListUserBindingsServlet}, for listing RBAC user-bindings</li>
     * </ul>
     *
     * @param contextHandler the context to add the RBAC servlets to
     * @param path the URL path under which to add the RBAC servlets
     * @param styraRun the {@link StyraRun} instance to use for communicating with the Styra Run API
     * @param sessionManager the {@link SessionManager session-manager} to use for composing session information. May be <code>null</code>.
     * @param userProvider the {@link UserProvider user-provider} for enumerating {@link User users}. May be <code>null</code>.
     */
    public static void addRbacServlets(ServletContextHandler contextHandler,
                                       String path,
                                       StyraRun styraRun,
                                       SessionManager<TenantSession> sessionManager,
                                       UserProvider userProvider) {
        contextHandler.addServlet(
                new ServletHolder(new RbacRolesServlet(styraRun, sessionManager)),
                joinPath(path, "roles"));

        contextHandler.addServlet(
                new ServletHolder(new RbacListUserBindingsServlet(styraRun, sessionManager, userProvider)),
                joinPath(path, "user_bindings"));

        contextHandler.addServlet(
                new ServletHolder(new RbacUserBindingServlet(styraRun, sessionManager)),
                joinPath(path, "user_bindings/*"));
    }
}
