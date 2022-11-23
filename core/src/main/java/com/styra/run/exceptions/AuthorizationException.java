package com.styra.run.exceptions;

import com.styra.run.exceptions.StyraRunException;

public class AuthorizationException extends StyraRunException {
    public AuthorizationException(String message) {
        super(message);
    }

    public AuthorizationException(String message, Throwable cause) {
        super(message, cause);
    }
}
