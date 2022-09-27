package com.styra.run;

import com.styra.run.StyraRun.BatchQuery;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

        handleAsync(request, response, (body, out, async) -> {
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
                            });
                });
    }
}
