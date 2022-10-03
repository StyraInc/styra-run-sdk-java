package com.styra.run;

public class RetryException extends Exception {
    public RetryException(Throwable cause) {
        super(cause);
    }
}
