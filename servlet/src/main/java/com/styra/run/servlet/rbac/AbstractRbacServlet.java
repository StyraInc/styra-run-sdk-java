package com.styra.run.servlet.rbac;

import com.styra.run.Input;
import com.styra.run.Result;
import com.styra.run.SerializableAsMap;
import com.styra.run.StyraRun;
import com.styra.run.exceptions.AuthorizationException;
import com.styra.run.rbac.RbacManager;
import com.styra.run.servlet.pagination.Page;
import com.styra.run.servlet.StyraRunServlet;
import com.styra.run.servlet.session.SessionManager;
import com.styra.run.session.InputTransformer;
import com.styra.run.session.Session;
import com.styra.run.session.TenantSession;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static com.styra.run.utils.Null.firstNonNull;
import static java.util.Collections.singletonMap;

public class AbstractRbacServlet extends StyraRunServlet {
    public AbstractRbacServlet() {
        super();
    }

    protected AbstractRbacServlet(StyraRun styraRun, SessionManager<Session> sessionManager, InputTransformer<Session> inputTransformer) {
        super(styraRun, sessionManager, inputTransformer);
    }

    protected RbacManager getRbacManager() throws ServletException {
        return new RbacManager(getStyraRun());
    }

    protected TenantSession getSession(HttpServletRequest request)
            throws AuthorizationException {
        try {
            Session session = getSessionManager().getSession(request);
            if (session instanceof TenantSession) {
                return (TenantSession) session;
            }
            return TenantSession.from(getInputTransformer()
                    .transform(Input.empty(), RbacManager.AUTHZ_PATH, session));
        } catch (Throwable t) {
            throw new AuthorizationException("Failed to form authorization input from session data", t);
        }
    }

    protected void writeResult(Object value, HttpServletResponse response, ServletOutputStream out, AsyncContext context) {
        writeResult(value, null, response, out, context);
    }

    protected void writeResult(Object value, Page page, HttpServletResponse response, ServletOutputStream out, AsyncContext context) {
        Result<?> result = new Result<>(SerializableAsMap.serialize(value));
        if (page != null) {
            result = result.withAttributes(singletonMap("page", page.serialize()));
        }
        writeOkJsonResponse(result.toMap(), response, out, context);
    }

    protected String getPath(HttpServletRequest request) {
        return firstNonNull(request.getPathInfo(), "/");
    }
}
