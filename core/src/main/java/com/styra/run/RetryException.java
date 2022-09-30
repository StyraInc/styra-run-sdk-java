package com.styra.run;

public class RetryException extends StyraRunException {
    public RetryException(String message) {
        super(message);
    }

    public RetryException(String message, Throwable cause) {
        super(message, cause);
    }
}
