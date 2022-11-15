package com.styra.run.session;

import com.styra.run.Input;

@FunctionalInterface
public interface InputTransformer<S extends Session> {
    Input<?> transform(Input<?> input, String path, S session);

    /**
     * An {@link InputTransformer} that returns the same {@link Input} instance it receives.
     *
     * @param <S> the {@link Session} type
     * @return an identity transformer
     */
    static <S extends Session> InputTransformer<S> identity() {
        return (input, path, session) -> input;
    }
}