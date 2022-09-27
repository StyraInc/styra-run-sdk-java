package com.styra.run;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;

import java.util.Optional;

import static com.styra.run.Utils.Null.cast;

public abstract class StyraRunServlet extends HttpServlet {
    public static final String STYRA_RUN_ATTR = "com.styra.run.styra-run";

    protected final StyraRun styraRun;

    public StyraRunServlet() {
        this.styraRun = null;
    }

    public StyraRunServlet(StyraRun styraRun) {
        this.styraRun = styraRun;
    }

    protected StyraRun getStyraRun() throws ServletException {
        if (styraRun != null) {
            return styraRun;
        }
        return Optional.ofNullable(cast(StyraRun.class, getServletConfig().getServletContext().getAttribute(STYRA_RUN_ATTR),
                        () -> new ServletException(String.format("'%s' attribute on servlet context was not StyraRun type", STYRA_RUN_ATTR))))
                .orElseThrow(() -> new ServletException(String.format("No '%s' attribute on servlet context", STYRA_RUN_ATTR)));
    }

//    protected static void handleAsync(HttpServletRequest request) {
//        AsyncContext async = request.startAsync();
//    }
}
