package com.styra.run.servlet;

import com.styra.run.BatchQuery;
import com.styra.run.Json;
import com.styra.run.Proxy;
import com.styra.run.StyraRun;
import com.styra.run.servlet.session.SessionManager;
import com.styra.run.session.InputTransformer;
import com.styra.run.session.Session;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Optional;

import static com.styra.run.utils.Types.cast;

/**
 * A servlet wrapping the functionality provided by {@link Proxy}.
 * <p>
 * If the servlet isn't directly instantiated by constructor, the following service can be injected by attribute:
 *
 * <ul>
 *     <li>
 *          {@link #INPUT_TRANSFORMER_ATTR}: {@link InputTransformer}
 *          <br>
 *          Optional; {@link InputTransformer#identity()} is used by default.
 *     </li>
 * </ul>
 * <p>
 * Please also see {@link StyraRunServlet} for additional services that can be injected by attribute.
 *
 * @see StyraRunServlet
 */
public final class ProxyServlet<S extends Session> extends StyraRunServlet<S> {
    public static final String INPUT_TRANSFORMER_ATTR = "com.styra.run.input-transformer";

    private static final InputTransformer<Session> DEFAULT_INPUT_TRANSFORMER = InputTransformer.identity();

    private volatile InputTransformer<S> inputTransformer;
    private volatile Proxy<S> proxy = null;

    public ProxyServlet() {
        super();
    }

    public ProxyServlet(StyraRun styraRun,
                        SessionManager<S> sessionManager,
                        InputTransformer<S> inputTransformer) {
        super(styraRun, sessionManager);
        this.inputTransformer = inputTransformer;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        StyraRun styraRun = getStyraRun();
        Json json = styraRun.getJson();
        SessionManager<S> sessionManager = getSessionManager();

        Proxy<S> proxy = getProxy();

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

    private InputTransformer<S> getInputTransformer() throws ServletException {
        if (inputTransformer == null) {

            //noinspection unchecked
            inputTransformer = Optional.ofNullable(cast(InputTransformer.class, getServletConfig().getServletContext().getAttribute(INPUT_TRANSFORMER_ATTR),
                            () -> new ServletException(String.format("'%s' attribute on servlet context was not InputTransformer type", INPUT_TRANSFORMER_ATTR))))
                    .orElse(DEFAULT_INPUT_TRANSFORMER);
        }
        return inputTransformer;
    }

    private Proxy<S> getProxy() throws ServletException {
        if (proxy == null) {
            InputTransformer<S> inputTransformer = getInputTransformer();
            proxy = Proxy.<S>builder(styraRun)
                    .inputTransformer(inputTransformer)
                    .build();
        }
        return proxy;
    }
}
