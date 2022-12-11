package com.styra.run.servlet.session;

import com.styra.run.Input;
import com.styra.run.session.Session;
import jakarta.servlet.http.HttpServletRequest;

/**
 * A session manager that produces no {@link Session} for incoming requests, and echoes back
 * the incoming {@link Input} on transformations.
 */
public class NoSessionManager<S extends Session> implements SessionManager<S> {
    @SuppressWarnings("rawtypes")
    public static NoSessionManager INSTANCE = new NoSessionManager();

    @SuppressWarnings("unchecked")
    public static <S extends Session> SessionManager<S> getInstance() {
        return (SessionManager<S>) NoSessionManager.INSTANCE;
    }

    @Override
    public Input<?> transform(Input<?> input, String path, S session) {
        return input;
    }

    @Override
    public S getSession(HttpServletRequest request) {
        return null;
    }
}
