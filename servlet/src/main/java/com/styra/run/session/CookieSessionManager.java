package com.styra.run.session;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;

import static java.util.Objects.requireNonNull;

public class CookieSessionManager implements SessionManager<TenantSession> {
    private final String cookieName;

    public CookieSessionManager(String cookieName) {
        this.cookieName = requireNonNull(cookieName, "cookieName must not be null");
    }

    @Override
    public TenantSession getSession(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            return Arrays.stream(cookies)
                    .filter(cookie -> cookieName.equals(cookie.getName()))
                    .findAny()
                    .map(cookie -> {
                        String[] components = cookie.getValue().split("\\s*/\\s*", 2);
                        String tenant = null;
                        if (components.length >= 1) {
                            tenant = components[0].trim();
                        }
                        String subject = null;
                        if (components.length >= 2) {
                            subject = components[1].trim();
                        }
                        return new TenantSession(subject, tenant);
                    })
                    .orElse(null);
        }
        return null;
    }
}