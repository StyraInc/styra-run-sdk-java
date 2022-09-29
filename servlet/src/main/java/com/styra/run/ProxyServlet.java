package com.styra.run;

import com.styra.run.StyraRun.BatchQuery;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public final class ProxyServlet extends StyraRunServlet {
    public ProxyServlet() {
        super();
    }

    public ProxyServlet(StyraRun styraRun) {
        super(styraRun);
    }

    public ProxyServlet(StyraRun styraRun, InputTransformer inputTransformer) {
        super(styraRun, inputTransformer);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        StyraRun styraRun = getStyraRun();
        Json json = styraRun.getJson();
        InputTransformer inputTransformer = getInputTransformer();

        handleAsync(request, response, (body, out, async) -> {
                    BatchQuery query = BatchQuery.fromMap(json.toMap(body));
                    List<StyraRun.Query> items = query.getItems().stream()
                            .map((q) -> new StyraRun.Query(q.getPath(),
                                    inputTransformer.transform(q.getInput(), q.getPath(), request)))
                            .collect(Collectors.toList());
                    Input<?> globalInput = inputTransformer.transform(query.getInput(), null, request);

                    styraRun.batchQuery(items, globalInput)
                            .thenAccept((result) ->
                                    writeOkJsonResponse(result.withoutAttributes().toMap(), response, out, async));
                });
    }
}
