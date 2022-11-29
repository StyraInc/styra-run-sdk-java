package com.styra.run.servlet.rbac;

import com.styra.run.StyraRun;
import com.styra.run.rbac.RbacManager;
import com.styra.run.servlet.StyraRunServlet;
import com.styra.run.servlet.session.SessionManager;
import com.styra.run.session.TenantSession;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * A servlet for retrieving the list of <code>roles</code> available in a <code>tenant</code>,
 * as defined by a Styra Run <code>project environment</code>.
 * <p>
 * E.g.
 * <p>
 * Getting the user-binding for <code>alice</code>:
 * <pre>
 * GET /roles
 * ->
 * 200 OK
 * {
 *    "result": [
 *       "ADMIN",
 *       "VIEWER",
 *       "EDITOR"
 *    ]
 * }
 * </pre>
 *
 * @see StyraRunServlet
 */
public class RbacRolesServlet extends AbstractRbacServlet {
    public RbacRolesServlet() {
        super();
    }

    public RbacRolesServlet(StyraRun styraRun,
                            SessionManager<TenantSession> sessionManager) {
        super(styraRun, sessionManager);
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
