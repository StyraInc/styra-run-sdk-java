package com.styra.run.session;

import com.styra.run.Input;

/**
 * Implementing types encapsulates authorization data required to make queries,
 * usually incorporated into the policy rule input.
 */
public interface Session {
    Input<?> toInput();
}
