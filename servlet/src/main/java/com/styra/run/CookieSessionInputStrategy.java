package com.styra.run;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public final class CookieSessionInputStrategy implements InputTransformer {
    private static final String TENANT_ATTR = "tenant";
    private static final String SUBJECT_ATTR = "subject";

    private final String cookieName;

    public CookieSessionInputStrategy(String cookieName) {
        Objects.requireNonNull(cookieName, "cookieName must not be null");
        this.cookieName = cookieName;
    }

    @Override
    public Input<?> transform(Input<?> input, String path, HttpServletRequest request) {
        MapInput.Builder<String, Object> inputBuilder = new MapInput.Builder<>();

        if (input != null && !input.isEmpty()) {
            Map<?, ?> inputValue = Utils.Null.safeCast(Map.class, input.getValue());
            if (inputValue == null) {
                // The given input isn't a map, so we can't inject session info into it.
                return input;
            }
            inputValue.forEach((key, value) -> inputBuilder.put(key.toString(), value));
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            Arrays.stream(cookies)
                    .filter(cookie -> cookieName.equals(cookie.getName()))
                    .findAny()
                    .ifPresent(cookie -> {
                        String[] components = cookie.getValue().split("\\s*/\\s*", 2);
                        if (components.length >= 1) {
                            inputBuilder.put(TENANT_ATTR, components[0].trim());
                        }
                        if (components.length >= 2) {
                            inputBuilder.put(SUBJECT_ATTR, components[1].trim());
                        }
                    });
        }

        return inputBuilder.build();
    }
}
