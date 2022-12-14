package com.styra.run.exceptions;

import com.styra.run.ApiError;
import com.styra.run.exceptions.StyraRunException;

public class StyraRunHttpException extends StyraRunException {
    // FIXME: Use ApiClient.ApiResponse instead?
    private final int statusCode;
    private final String body;
    private final ApiError apiError;

    public StyraRunHttpException(int statusCode, String body) {
        this(statusCode, body, null);
    }

    public StyraRunHttpException(int statusCode, String body, ApiError apiError) {
        super(String.format("Unexpected status code: %d", statusCode));
        this.statusCode = statusCode;
        this.body = body;
        this.apiError = apiError;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getBody() {
        return body;
    }

    public ApiError getApiError() {
        return apiError;
    }
}
