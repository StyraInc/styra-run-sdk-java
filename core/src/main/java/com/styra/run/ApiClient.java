package com.styra.run;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface ApiClient {
    class ApiResponse {
        private final int statusCode;
        private final String body;

        public ApiResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        public boolean isSuccessful() {
            // Not considering 3XX as successful
            return statusCode >= 200 && statusCode < 300;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getBody() {
            return body;
        }
    }

    CompletableFuture<ApiResponse> get(URI uri, Map<String, String> headers);

    CompletableFuture<ApiResponse> put(URI uri, String body, Map<String, String> headers);

    CompletableFuture<ApiResponse> post(URI uri, String body, Map<String, String> headers);

    CompletableFuture<ApiResponse> delete(URI uri, Map<String, String> headers);
}
