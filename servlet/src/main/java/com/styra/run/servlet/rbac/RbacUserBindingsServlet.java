package com.styra.run.servlet.rbac;

import com.styra.run.StyraRun;
import com.styra.run.rbac.RbacManager;
import com.styra.run.servlet.session.SessionManager;
import com.styra.run.session.InputTransformer;
import com.styra.run.session.Session;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class RbacUserBindingsServlet extends AbstractRbacServlet {
    public RbacUserBindingsServlet() {
        super();
    }

    private RbacUserBindingsServlet(StyraRun styraRun,
                                    SessionManager<Session> sessionManager,
                                    InputTransformer<Session> inputTransformer) {
        super(styraRun, sessionManager, inputTransformer);
    }

    public static <S extends Session> RbacUserBindingsServlet from(StyraRun styraRun,
                                                                   SessionManager<S> sessionManager,
                                                                   InputTransformer<S> inputTransformer) {
        //noinspection unchecked
        return new RbacUserBindingsServlet(styraRun,
                (SessionManager<Session>) sessionManager,
                (InputTransformer<Session>) inputTransformer);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        RbacManager rbac = getRbacManager();
        handleAsync(request, response, (body, out, async) ->
                rbac.listUserBindings(getSession(request))
                        .thenAccept((bindings) -> writeResult(bindings, response, out, async))
                        .exceptionally((e) -> {
                            handleError("Failed to GET user bindings for", e, async, response);
                            return null;
                        }));

    }
}
