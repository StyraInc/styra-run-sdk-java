package com.styra.run;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 *
 */
public final class ProxyServlet extends StyraRunServlet {
    private volatile Proxy<Proxy.Session> proxy = null;

    public ProxyServlet() {
        super();
    }

    private ProxyServlet(StyraRun styraRun, SessionManager<Proxy.Session> sessionManager, Proxy.InputTransformer<Proxy.Session> inputTransformer) {
        super(styraRun, sessionManager, inputTransformer);
    }

    public static <S extends Proxy.Session> ProxyServlet from(StyraRun styraRun, SessionManager<S> sessionManager, Proxy.InputTransformer<S> inputTransformer) {
        //noinspection unchecked
        return new ProxyServlet(styraRun,
                (SessionManager<Proxy.Session>) sessionManager,
                (Proxy.InputTransformer<Proxy.Session>) inputTransformer);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        StyraRun styraRun = getStyraRun();
        Json json = styraRun.getJson();
        SessionManager<Proxy.Session> sessionManager = getSessionManager();

        Proxy<Proxy.Session> proxy = getProxy();

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

    private Proxy<Proxy.Session> getProxy() throws ServletException {
        if (proxy == null) {
            Proxy.InputTransformer<Proxy.Session> inputTransformer = getInputTransformer();
            proxy = new Proxy<>(styraRun, inputTransformer);
        }
        return proxy;
    }
}
