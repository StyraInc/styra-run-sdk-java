package com.styra.run.test;

import com.styra.run.InputContainer;
import com.styra.run.Json;
import com.styra.run.Result;
import com.styra.run.StyraRun;
import com.styra.run.servlet.StyraRunServlet;
import com.styra.run.session.Session;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import static com.styra.run.utils.Null.firstNonNull;

public class CheckProxyServlet extends StyraRunServlet<Session> {
    public CheckProxyServlet(StyraRun styraRun) {
        super(styraRun, null);
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
            styraRun.check(path, input)
                    .thenApply(Result::new)
                    .thenAccept(result -> writeOkJsonResponse(result.toMap(), response, out, async))
                    .exceptionally(err -> {
                        handleError("Query failed", err, async, response);
                        return null;
                    });
        });
    }
}
