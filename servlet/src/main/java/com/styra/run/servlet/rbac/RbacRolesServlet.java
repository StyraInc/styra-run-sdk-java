package com.styra.run.servlet.rbac;

import com.styra.run.StyraRun;
import com.styra.run.rbac.RbacManager;
import com.styra.run.servlet.session.SessionManager;
import com.styra.run.session.TenantSession;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

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
