package com.styra.run;

public class ApiResponse {
    private final int statusCode;
    private final String body;

    public ApiResponse(int statusCode, String body) {
        this.statusCode = statusCode;
        this.body = body;
    }

    public boolean isSuccessful() {
        // Not considering 3XX as successful, as these should be handled by the ApiClient, and never surfaced
        return statusCode >= 200 && statusCode < 300;
    }

    public boolean isNotFoundStatus() {
        return statusCode == 404;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getBody() {
        return body;
    }

    @Override
    public String toString() {
        return "ApiResponse{" +
                "statusCode=" + statusCode +
                ", body='" + body + '\'' +
                '}';
    }
}
