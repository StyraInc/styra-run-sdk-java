package com.styra.run.servlet;

import com.styra.run.ApiError;
import com.styra.run.StyraRun;
import com.styra.run.exceptions.AuthorizationException;
import com.styra.run.servlet.session.SessionManager;
import com.styra.run.session.Session;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;

import static com.styra.run.ApiError.BAD_REQUEST_CODE;
import static com.styra.run.ApiError.INTERNAL_ERROR_CODE;
import static com.styra.run.ApiError.UNAUTHORIZED_CODE;
import static com.styra.run.utils.Types.cast;

/**
 * An abstract, asynchronous servlet implemented by all Styra Run SDK servlets.
 * <p>
 * If the servlet isn't directly instantiated by constructor, the following services can be injected by attribute:
 *
 * <ul>
 *     <li>
 *          {@link #STYRA_RUN_ATTR}: {@link StyraRun}
 *          <br>
 *          Required.
 *     </li>
 *     <li>
 *          {@link #SESSION_MANAGER_ATTR}: {@link SessionManager}
 *          <br>
 *          Optional; {@link SessionManager#noSessionManager()} is used by default.
 *     </li>
 * </ul>
 */
public abstract class StyraRunServlet<S extends Session> extends HttpServlet {
    public static final String STYRA_RUN_ATTR = "com.styra.run.styra-run";
    public static final String SESSION_MANAGER_ATTR = "com.styra.run.session-manager";
    private static final SessionManager<Session> DEFAULT_SESSION_MANAGER = SessionManager.noSessionManager();

    protected final StyraRun styraRun;
    private volatile SessionManager<S> sessionManager;



    public StyraRunServlet() {
        this(null, null);
    }

    protected StyraRunServlet(StyraRun styraRun, SessionManager<S> sessionManager) {
        this.styraRun = styraRun;
        this.sessionManager = sessionManager;
    }

    protected StyraRun getStyraRun() throws ServletException {
        if (styraRun != null) {
            return styraRun;
        }
        return Optional.ofNullable(cast(StyraRun.class, getServletConfig().getServletContext().getAttribute(STYRA_RUN_ATTR),
                        () -> new ServletException(String.format("'%s' attribute on servlet context was not StyraRun type", STYRA_RUN_ATTR))))
                .orElseThrow(() -> new ServletException(String.format("No '%s' attribute on servlet context", STYRA_RUN_ATTR)));
    }

    protected SessionManager<S> getSessionManager() throws ServletException {
        if (sessionManager == null) {

            //noinspection unchecked
            sessionManager = Optional.ofNullable(cast(SessionManager.class, getServletConfig().getServletContext().getAttribute(SESSION_MANAGER_ATTR),
                            () -> new ServletException(String.format("'%s' attribute on servlet context was not SessionManager type", SESSION_MANAGER_ATTR))))
                    .orElse(DEFAULT_SESSION_MANAGER);
        }
        return sessionManager;
    }

    protected void handleAsync(HttpServletRequest request, HttpServletResponse response, OnReady onReady) throws IOException {
        AsyncContext async = request.startAsync();

        ServletInputStream in = request.getInputStream();
        in.setReadListener(new AsyncReader(async, response, in, onReady));
    }

    @FunctionalInterface
    protected interface OnReady {
        void accept(String body, ServletOutputStream out, AsyncContext async) throws Exception;
    }

    private class AsyncReader implements ReadListener {
        private final AsyncContext context;
        private final HttpServletResponse response;
        private final ServletInputStream in;
        private final List<String> parts = new LinkedList<>();
        private final OnReady onReady;

        private AsyncReader(AsyncContext context,
                            HttpServletResponse response,
                            ServletInputStream in,
                            OnReady onReady) {
            this.context = context;
            this.response = response;
            this.in = in;
            this.onReady = onReady;
        }

        @Override
        public void onDataAvailable() throws IOException {
            int len;
            byte[] bytes = new byte[1024];

            while (in.isReady() && (len = in.read(bytes)) != -1) {
                String data = new String(bytes, 0, len);
                parts.add(data);
            }
        }

        @Override
        public void onAllDataRead() throws IOException {
            String input = String.join("", parts);
            ServletOutputStream out = response.getOutputStream();
            out.setWriteListener(new AsyncWriter(context, response, out, input, onReady));
        }

        @Override
        public void onError(Throwable t) {
            handleError("Read error", t, context, response);
        }
    }

    private class AsyncWriter implements WriteListener {
        private final AsyncContext context;
        private final HttpServletResponse response;
        private final ServletOutputStream out;
        private final String input;
        private final OnReady onReady;

        private AsyncWriter(AsyncContext context,
                            HttpServletResponse response,
                            ServletOutputStream out,
                            String input,
                            OnReady onReady) {
            this.context = context;
            this.response = response;
            this.out = out;
            this.input = input;
            this.onReady = onReady;
        }

        @Override
        public void onWritePossible() {
            try {
                onReady.accept(input, out, context);
            } catch (Exception e) {
                onError(e);
            }
        }

        @Override
        public void onError(Throwable t) {
            handleError("Write error", t, context, response);
        }
    }

    protected void handleError(String message, Throwable t, AsyncContext context, HttpServletResponse response) {
        if (t instanceof CompletionException) {
            handleError(message, t.getCause(), context, response);
            return;
        }

        getServletContext().log(message, t);
        if (t instanceof AuthorizationException) {
            writeErrorJsonResponse(new ApiError(UNAUTHORIZED_CODE, "Unauthorized"),
                    response, HttpServletResponse.SC_FORBIDDEN, context);
        } if (t instanceof BadRequestException) {
            writeErrorJsonResponse(new ApiError(BAD_REQUEST_CODE, "Bad request"),
                    response, HttpServletResponse.SC_BAD_REQUEST, context);
        } else {
            writeErrorJsonResponse(new ApiError(INTERNAL_ERROR_CODE, "Internal server error"),
                    response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, context);
        }
    }

    protected void writeErrorJsonResponse(ApiError error, HttpServletResponse response, int statusCode, AsyncContext context) {
        try {
            response.setStatus(statusCode);
            response.setContentType("application/json");
            response.getOutputStream()
                    .write(getStyraRun().getJson().from(error.toMap()).getBytes(StandardCharsets.UTF_8));
        } catch (IOException | ServletException e) {
            getServletContext().log("Failed to send JSON error response", e);
        } finally {
            context.complete();
        }
    }

    protected void writeOkJsonResponse(Object data, HttpServletResponse response, ServletOutputStream out, AsyncContext context) {
        try {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            out.write(getStyraRun().getJson().from(data).getBytes(StandardCharsets.UTF_8));
        } catch (IOException | ServletException e) {
            handleError("Failed to send JSON response", e, context, response);
        } finally {
            context.complete();
        }
    }
}
