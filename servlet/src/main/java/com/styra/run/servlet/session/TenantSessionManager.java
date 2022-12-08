package com.styra.run.servlet.session;

import com.styra.run.session.TenantInputTransformer;
import com.styra.run.session.TenantSession;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;
import java.util.function.Function;

/**
 * A convenience {@link SessionManager}, for converting a map of session data into a {@link TenantSession}.
 */
public class TenantSessionManager extends TenantInputTransformer
        implements SessionManager<TenantSession> {
    private final Function<HttpServletRequest, Map<String, Object>> sessionDataProvider;

    /**
     * The map returned by <code>sessionDataProvider</code> must contain a <code>"tenant"</code> entry
     * in order to produce a valid {@link TenantSession}.
     *
     * @param sessionDataProvider provider of session data
     */
    public TenantSessionManager(Function<HttpServletRequest, Map<String, Object>> sessionDataProvider) {
        this.sessionDataProvider = sessionDataProvider;
    }

    @Override
    public TenantSession getSession(HttpServletRequest request) {
        return new TenantSession(sessionDataProvider.apply(request));
    }
}
