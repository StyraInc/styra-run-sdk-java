package com.styra.run.session;

import jakarta.servlet.http.HttpServletRequest;

public interface SessionManager<S extends Session> {
    S getSession(HttpServletRequest request);

    static <S extends Session> SessionManager<S> noSessionManager() {
        return (request) -> null;
    }
}
