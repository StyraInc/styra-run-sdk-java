package com.styra.run;

public class StyraRunException extends Exception {
    public StyraRunException(String message) {
        super(message);
    }

    public StyraRunException(String message, Throwable cause) {
        super(message, cause);
    }
}
