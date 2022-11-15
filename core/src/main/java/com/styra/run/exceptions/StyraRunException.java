package com.styra.run.exceptions;

public class StyraRunException extends Exception {
    public StyraRunException(String message) {
        super(message);
    }

    public StyraRunException(String message, Throwable cause) {
        super(message, cause);
    }
}
