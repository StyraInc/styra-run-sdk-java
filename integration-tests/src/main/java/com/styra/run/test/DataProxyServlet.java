package com.styra.run.test;

import com.styra.run.StyraRun;
import com.styra.run.servlet.StyraRunServlet;
import com.styra.run.session.Session;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import static com.styra.run.utils.Null.firstNonNull;

public class DataProxyServlet extends StyraRunServlet<Session> {
    public DataProxyServlet(StyraRun styraRun) {
        super(styraRun, null);
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        var styraRun = getStyraRun();
        var path = firstNonNull(request.getPathInfo(), "/");

        handleAsync(request, response, (body, out, async) -> {
            styraRun.getData(path)
                    .thenAccept(result -> writeOkJsonResponse(result.toMap(), response, out, async))
                    .exceptionally(err -> {
                        handleError("GET data failed", err, async, response);
                        return null;
                    });
        });
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        var styraRun = getStyraRun();
        var json = styraRun.getJson();
        var path = firstNonNull(request.getPathInfo(), "/");

        handleAsync(request, response, (body, out, async) -> {
            var data = json.toOptional(Object.class, body).orElse(null);
            styraRun.putData(path, data)
                    .thenAccept(result -> writeOkJsonResponse(result.toMap(), response, out, async))
                    .exceptionally(err -> {
                        handleError("PUT data failed", err, async, response);
                        return null;
                    });
        });
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        var styraRun = getStyraRun();
        var path = firstNonNull(request.getPathInfo(), "/");

        handleAsync(request, response, (body, out, async) -> {
            styraRun.deleteData(path)
                    .thenAccept(result -> writeOkJsonResponse(result.toMap(), response, out, async))
                    .exceptionally(err -> {
                        handleError("DELETE data failed", err, async, response);
                        return null;
                    });
        });
    }
}
