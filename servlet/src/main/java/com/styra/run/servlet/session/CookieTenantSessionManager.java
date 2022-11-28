package com.styra.run.servlet.session;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

// FIXME: Remove?
public class CookieTenantSessionManager extends TenantSessionManager {
    public static final String DEFAULT_COOKIE_NAME = "user";

    public CookieTenantSessionManager() {
        this(DEFAULT_COOKIE_NAME);
    }

    public CookieTenantSessionManager(String cookieName) {
        super(makeSessionDataProvider(requireNonNull(cookieName,
                "cookieName must not be null")));
    }

    private static Function<HttpServletRequest, Map<String, Object>> makeSessionDataProvider(String cookieName) {
        return (request) -> {
            Cookie[] cookies = request.getCookies();
            return Arrays.stream(cookies != null ? cookies : new Cookie[]{})
                    .filter(cookie -> cookieName.equals(cookie.getName()))
                    .findAny()
                    .map(cookie -> {
                        String[] components = cookie.getValue().split("\\s*/\\s*", 2);
                        Map<String, Object> map = new HashMap<>();
                        if (components.length > 0) {
                            map.put("tenant", components[0].trim());
                        }
                        if (components.length > 1) {
                            map.put("subject", components[1].trim());
                        }
                        return map;
                    }).orElse(Collections.emptyMap());
        };
    }
}
