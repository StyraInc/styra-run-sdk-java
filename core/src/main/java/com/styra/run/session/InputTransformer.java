package com.styra.run.session;

import com.styra.run.Input;

@FunctionalInterface
public interface InputTransformer<S extends Session> {
    Input<?> transform(Input<?> input, String path, S session);

    static <S extends Session> InputTransformer<S> identity() {
        return (input, path, session) -> input;
    }
}
