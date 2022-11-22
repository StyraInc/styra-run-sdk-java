package com.styra.run.test;

import com.styra.run.InputContainer;
import com.styra.run.Json;
import com.styra.run.StyraRun;
import com.styra.run.servlet.StyraRunServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import static com.styra.run.utils.Null.firstNonNull;

public class QueryProxyServlet extends StyraRunServlet {
    public QueryProxyServlet(StyraRun styraRun) {
        super(styraRun, null, null);
    }

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        StyraRun styraRun = getStyraRun();
        Json json = styraRun.getJson();
        var path = firstNonNull(request.getPathInfo(), "/");

        handleAsync(request, response, (body, out, async) -> {
            var input = json.toOptionalMap(body)
                    .map(InputContainer::fromMap)
                    .map(InputContainer::getInput)
                    .orElse(null);
            styraRun.query(path, input)
                    .thenAccept(result -> writeOkJsonResponse(result.toMap(), response, out, async))
                    .exceptionally((e) -> {
                        handleError("Query failed", e, async, response);
                        return null;
                    });
        });
    }
}
