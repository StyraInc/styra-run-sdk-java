package com.styra.run;

import com.styra.run.session.InputTransformer;
import com.styra.run.session.Session;
import com.styra.run.session.SessionManager;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 *
 */
public final class ProxyServlet extends StyraRunServlet {
    private volatile Proxy<Session> proxy = null;

    public ProxyServlet() {
        super();
    }

    private ProxyServlet(StyraRun styraRun, SessionManager<Session> sessionManager, InputTransformer<Session> inputTransformer) {
        super(styraRun, sessionManager, inputTransformer);
    }

    public static <S extends Session> ProxyServlet from(StyraRun styraRun,
                                                        SessionManager<S> sessionManager,
                                                        InputTransformer<S> inputTransformer) {
        //noinspection unchecked
        return new ProxyServlet(styraRun,
                (SessionManager<Session>) sessionManager,
                (InputTransformer<Session>) inputTransformer);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        StyraRun styraRun = getStyraRun();
        Json json = styraRun.getJson();
        SessionManager<Session> sessionManager = getSessionManager();

        Proxy<Session> proxy = getProxy();

        handleAsync(request, response, (body, out, async) -> {
            BatchQuery query = BatchQuery.fromMap(json.toMap(body));

            proxy.proxy(query, sessionManager.getSession(request))
                    .thenAccept((result) ->
                            writeOkJsonResponse(result.withoutAttributes().toMap(), response, out, async))
                    .exceptionally((e) -> {
                        handleError("Batch query failed", e, async, response);
                        return null;
                    });
        });
    }

    private Proxy<Session> getProxy() throws ServletException {
        if (proxy == null) {
            InputTransformer<Session> inputTransformer = getInputTransformer();
            proxy = Proxy.builder(styraRun)
                    .inputTransformer(inputTransformer)
                    .build();
        }
        return proxy;
    }
}
