package com.styra.run;

import com.styra.run.StyraRun.BatchQuery;
import com.styra.run.Utils.Lambda.CheckedBiConsumer;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.styra.run.Utils.Null.cast;
import static java.util.Collections.singletonMap;

public class ProxyServlet extends HttpServlet {
    public final String STYRA_RUN_ATTR = "styra-run";
    public final String INPUT_SUPPLIER_ATTR = "input-supplier";

    private final InputSupplier DEFAULT_INPUT_SUPPLIER = (path, input, request) -> input;

    private final StyraRun styraRun;
    private final InputSupplier inputSupplier;

    public ProxyServlet() {
        this.inputSupplier = null;
        this.styraRun = null;
    }

    public ProxyServlet(StyraRun styraRun, InputSupplier inputSupplier) {
        this.styraRun = styraRun;
        this.inputSupplier = inputSupplier;
    }

    private StyraRun getStyraRun() throws ServletException {
        if (styraRun != null) {
            return styraRun;
        }
        return Optional.ofNullable(cast(StyraRun.class, getServletConfig().getServletContext().getAttribute(STYRA_RUN_ATTR),
                        () -> new ServletException(String.format("'%s' attribute on servlet context was not StyraRun type", STYRA_RUN_ATTR))))
                .orElseThrow(() -> new ServletException(String.format("No '%s' attribute on servlet context", STYRA_RUN_ATTR)));
    }

    private InputSupplier getInputSupplier() throws ServletException {
        if (inputSupplier != null) {
            return inputSupplier;
        }
        return Optional.ofNullable(cast(InputSupplier.class, getServletConfig().getServletContext().getAttribute("input-supplier"),
                        () -> new ServletException(String.format("'%s' attribute on servlet context was not InputSupplier type", INPUT_SUPPLIER_ATTR))))
                .orElse(DEFAULT_INPUT_SUPPLIER);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        StyraRun styraRun = getStyraRun();
        InputSupplier inputSupplier = getInputSupplier();
        AsyncContext async = request.startAsync();

        ServletInputStream in = request.getInputStream();
        in.setReadListener(new AsyncReader(async, response, in,
                (body, write) -> {
                    BatchQuery query = styraRun.getJson().to(BatchQuery.class, body);
                    List<StyraRun.Query> items = query.getItems().stream()
                            .map((q) -> new StyraRun.Query(q.getPath(),
                                    inputSupplier.get(null, q.getInput(), request)))
                            .collect(Collectors.toList());
                    Object globalInput = inputSupplier.get(null, query.getInput(), request);

                    styraRun.batchQuery(items, globalInput)
                            .thenAccept((result) -> {
                                List<?> output = result.get().stream()
                                        .map(ProxyServlet::resultToMap)
                                        .collect(Collectors.toList());
                                try {
                                    write.accept(styraRun.getJson().from(singletonMap("result", output))
                                            .getBytes(StandardCharsets.UTF_8));
                                } catch (IOException e) {
                                    handleError("Failed to marshal JSON response", e, async, response);
                                }
                                response.setStatus(200);
                                async.complete();
                            }).join();
                }));
    }

    private static Map<?, ?> resultToMap(Result<?> result) {
        Object value = result.get();
        if (value == null) {
            return Collections.emptyMap();
        }
        return singletonMap("result", value);
    }

    private interface OnReady extends CheckedBiConsumer<String, Write, IOException> {
    }

    private interface Write extends Consumer<byte[]> {
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
        public void onWritePossible() throws IOException {
            onReady.accept(input, (bytes) -> {
                try {
                    out.write(bytes);
                } catch (IOException e) {
                    onError(e);
                }
            });
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
