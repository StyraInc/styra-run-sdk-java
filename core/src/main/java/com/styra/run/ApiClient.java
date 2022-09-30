package com.styra.run;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface ApiClient {
    CompletableFuture<ApiResponse> get(URI uri, Map<String, String> headers);

    CompletableFuture<ApiResponse> put(URI uri, String body, Map<String, String> headers);

    CompletableFuture<ApiResponse> post(URI uri, String body, Map<String, String> headers);

    CompletableFuture<ApiResponse> delete(URI uri, Map<String, String> headers);

    // TODO: make the only request method
    default CompletableFuture<ApiResponse> request(Method method, URI uri, Map<String, String> headers, String body) {
        switch (method) {
            case GET: return get(uri, headers);
            case PUT: return put(uri, body, headers);
            case POST: return post(uri, body, headers);
            case DELETE: return delete(uri, headers);
            default: throw new IllegalArgumentException("Unknown method: " + method);
        }
    }

    default RequestBuilder requestBuilder(Method method) {
        return new RequestBuilder(this, method);
    }

    enum Method {
        GET,
        PUT,
        POST,
        DELETE
    }

    class RequestBuilder {
        private final ApiClient apiClient;
        private final Method method;
        private final Map<String, String> headers = new HashMap<>();
        private URI uri;
        private String body;

        public RequestBuilder(ApiClient apiClient, Method method) {
            this.apiClient = apiClient;
            this.method = method;
        }

        public RequestBuilder uri(URI uri) {
            this.uri = uri;
            return this;
        }

        public RequestBuilder headers(Map<String, String> headers) {
            this.headers.putAll(headers);
            return this;
        }

        public RequestBuilder header(String name, String value) {
            this.headers.put(name, value);
            return this;
        }

        public RequestBuilder jsonContentType() {
            this.headers.put("Content-Type", "application/json");
            return this;
        }

        public RequestBuilder body(String body) {
            this.body = body;
            return this;
        }

        public CompletableFuture<ApiResponse> request() {
            return apiClient.request(method, uri, headers, body);
        }
    }
}
