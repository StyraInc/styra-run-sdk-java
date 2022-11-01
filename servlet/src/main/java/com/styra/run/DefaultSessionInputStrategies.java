package com.styra.run;

public class DefaultSessionInputStrategies {
    public static final InputTransformer COOKIE = new CookieSessionInputStrategy("user");
    public static final InputTransformer REQUEST_ATTRIBUTES = InputTransformer.IDENTITY; // TODO
    public static final InputTransformer IDENTITY = InputTransformer.IDENTITY;
}
