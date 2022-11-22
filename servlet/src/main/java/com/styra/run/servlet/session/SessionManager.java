package com.styra.run.servlet.session;

import com.styra.run.session.Session;
import jakarta.servlet.http.HttpServletRequest;

/**
 * A session-manager retrieves {@link Session session} information from an incoming {@link HttpServletRequest HTTP request}.
 *
 * @param <S> the {@link Session} type
 */
public interface SessionManager<S extends Session> {
    S getSession(HttpServletRequest request);

    /**
     * Returns <code>null</code> when queried for a session.
     *
     * @return <code>null</code>
     */
    static <S extends Session> SessionManager<S> noSessionManager() {
        return (request) -> null;
    }
}
