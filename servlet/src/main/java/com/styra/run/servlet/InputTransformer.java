package com.styra.run.servlet;

import com.styra.run.Input;
import jakarta.servlet.http.HttpServletRequest;

@FunctionalInterface
public interface InputTransformer {
    InputTransformer IDENTITY = (input, path, request) -> input;
    Input<?> transform(Input<?> input, String path, HttpServletRequest request);
}
