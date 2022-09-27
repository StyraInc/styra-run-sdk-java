package com.styra.run;

public class DefaultSessionInputStrategies {
    public static final InputTransformer COOKIE = new CookieSessionInputStrategy("user");
}
