package com.styra.run;

import com.styra.run.StyraRun.BatchQuery;
import com.styra.run.Utils.Lambda.CheckedBiConsumer;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.styra.run.Utils.Null.cast;

public final class ProxyServlet extends StyraRunServlet {
    public static final String INPUT_TRANSFORMER_ATTR = "input-transformer";
    private final InputTransformer DEFAULT_INPUT_TRANSFORMER = DefaultSessionInputStrategies.COOKIE;

    private final InputTransformer inputTransformer;

    public ProxyServlet() {
        super();
        this.inputTransformer = null;
    }

    public ProxyServlet(StyraRun styraRun) {
        super(styraRun);
        this.inputTransformer = DEFAULT_INPUT_TRANSFORMER;
    }

    public ProxyServlet(StyraRun styraRun, InputTransformer inputTransformer) {
        super(styraRun);
        this.inputTransformer = inputTransformer;
    }

    private InputTransformer getInputSupplier() throws ServletException {
        if (inputTransformer != null) {
            return inputTransformer;
        }
        return Optional.ofNullable(cast(InputTransformer.class, getServletConfig().getServletContext().getAttribute("input-supplier"),
                        () -> new ServletException(String.format("'%s' attribute on servlet context was not InputSupplier type", INPUT_TRANSFORMER_ATTR))))
                .orElse(DEFAULT_INPUT_TRANSFORMER);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        StyraRun styraRun = getStyraRun();
        InputTransformer inputTransformer = getInputSupplier();
        AsyncContext async = request.startAsync();

        ServletInputStream in = request.getInputStream();
        in.setReadListener(new AsyncReader(async, response, in,
                (body, out) -> {
                    BatchQuery query = BatchQuery.fromMap(styraRun.getJson().toMap(body));
                    List<StyraRun.Query> items = query.getItems().stream()
                            .map((q) -> new StyraRun.Query(q.getPath(),
                                    inputTransformer.transform(q.getInput(), q.getPath(), request)))
                            .collect(Collectors.toList());
                    Input<?> globalInput = inputTransformer.transform(query.getInput(), null, request);

                    styraRun.batchQuery(items, globalInput)
                            .thenAccept((result) -> {
                                try {
                                    out.write(styraRun.getJson().from(result.withoutAttributes().toMap())
                                            .getBytes(StandardCharsets.UTF_8));
                                    response.setStatus(HttpServletResponse.SC_OK);
                                } catch (IOException e) {
                                    handleError("Failed to marshal JSON response", e, async, response);
                                } finally {
                                    async.complete();
                                }
                            }).join();
                }));
    }

    private interface OnReady extends CheckedBiConsumer<String, ServletOutputStream, IOException> {
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
                onReady.accept(input, out);
            } catch (IOException e) {
                onError(e);
            }
        }

        @Override
        public void onError(Throwable t) {
            handleError("Write error", t, context, response);
        }
    }

    private void handleError(String message, Throwable t, AsyncContext context, HttpServletResponse response) {
        try {
            getServletContext().log(message, t);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (IOException e) {
            getServletContext().log("Error", e);
        } finally {
            context.complete();
        }
    }
}
