package com.styra.run.servlet.session;

import com.styra.run.Input;
import com.styra.run.session.InputTransformer;
import com.styra.run.session.Session;
import jakarta.servlet.http.HttpServletRequest;

/**
 * A session-manager retrieves {@link Session session} information from an incoming {@link HttpServletRequest HTTP request}.
 *
 * @param <S> the {@link Session} type
 */
public interface SessionManager<S extends Session> extends InputTransformer<S> {
    S getSession(HttpServletRequest request);

    @Override
    default Input<?> transform(Input<?> input, String path, S session) {
        return null;
    };
}

