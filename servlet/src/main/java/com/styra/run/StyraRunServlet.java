package com.styra.run;

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

import static com.styra.run.utils.Types.cast;

public abstract class StyraRunServlet extends HttpServlet {
    public static final String STYRA_RUN_ATTR = "com.styra.run.styra-run";
    public static final String INPUT_TRANSFORMER_ATTR = "com.styra.run.input-transformer";
    private final InputTransformer DEFAULT_INPUT_TRANSFORMER = DefaultSessionInputStrategies.COOKIE;

    protected final StyraRun styraRun;
    private final InputTransformer inputTransformer;

    public StyraRunServlet() {
        this.styraRun = null;
        this.inputTransformer = null;
    }

    public StyraRunServlet(StyraRun styraRun) {
        this.styraRun = styraRun;
        this.inputTransformer = null;
    }

    public StyraRunServlet(StyraRun styraRun, InputTransformer inputTransformer) {
        this.styraRun = styraRun;
        this.inputTransformer = inputTransformer;
    }

    protected StyraRun getStyraRun() throws ServletException {
        if (styraRun != null) {
            return styraRun;
        }
        return Optional.ofNullable(cast(StyraRun.class, getServletConfig().getServletContext().getAttribute(STYRA_RUN_ATTR),
                        () -> new ServletException(String.format("'%s' attribute on servlet context was not StyraRun type", STYRA_RUN_ATTR))))
                .orElseThrow(() -> new ServletException(String.format("No '%s' attribute on servlet context", STYRA_RUN_ATTR)));
    }

    protected InputTransformer getInputTransformer() throws ServletException {
        if (inputTransformer != null) {
            return inputTransformer;
        }
        return Optional.ofNullable(cast(InputTransformer.class, getServletConfig().getServletContext().getAttribute("input-supplier"),
                        () -> new ServletException(String.format("'%s' attribute on servlet context was not InputSupplier type", INPUT_TRANSFORMER_ATTR))))
                .orElse(DEFAULT_INPUT_TRANSFORMER);
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

        try {
            getServletContext().log(message, t);
            if (t instanceof AuthorizationException) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
            } else {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } catch (IOException e) {
            getServletContext().log("Error", e);
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
            handleError("Failed to marshal JSON response", e, context, response);
        } finally {
            context.complete();
        }
    }
}
