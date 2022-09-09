package com.styra.run;

import jakarta.servlet.http.HttpServletRequest;

@FunctionalInterface
public interface InputSupplier {
    Object get(String path, Object input, HttpServletRequest request);
}
