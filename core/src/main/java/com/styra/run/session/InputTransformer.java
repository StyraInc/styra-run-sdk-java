package com.styra.run.session;

import com.styra.run.Input;

/**
 * The InputTransformer is an integration point for transforming incoming {@link Input input} to a policy query
 * given some {@link Session session}.
 * Usages include, but are not limited to, injecting session information - such as <code>subject</code> and <code>tenant</code> -
 * into the <code>input</code> value/document used when querying a policy rule.
 *
 * @param <S> the {@link Session} type
 */
@FunctionalInterface
public interface InputTransformer<S extends Session> {
    /**
     * Creates a new {@link Input}, given an incoming <code>input</code>, policy <code>path</code> and active <code>session</code>.
     *
     * @param input the incoming {@link Input input} value. May be <code>null</code>.
     * @param path the policy location (package and, optionally, rule). May be <code>null</code>.
     * @param session the active {@link Session session}. May be <code>null</code>.
     * @return the {@link Input} to use in a policy query. May be <code>null</code>.
     */
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
