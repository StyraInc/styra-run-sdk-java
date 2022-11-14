package com.styra.run;

import com.styra.run.Proxy.Session;
import jakarta.servlet.http.HttpServletRequest;

public interface SessionManager<S extends Session> {
    S getSession(HttpServletRequest request);

    static <S extends Session> SessionManager<S> noSessionManager() {
        return (request) -> null;
    }
}
