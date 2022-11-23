package com.styra.run;

import com.styra.run.exceptions.RetryException;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface ApiClient extends AutoCloseable {
    // TODO: package request params into ApiRequest object?

    /**
     * Performs a RESTful request to the Styra Run API.
     *
     * When a connection can't be established, the returned {@link CompletableFuture} must be
     * completed exceptionally with a {@link RetryException}, encapsulating an exception describing
     * the error.
     *
     * @param method the {@link Method} of the request to be executed
     * @param uri the {@link URI} of the request to be executed
     * @param headers the headers of the request to be executed
     * @param body the {@link String} body of the request to be executed, or <code>null</code> if none should be sent
     * @return a {@link CompletableFuture} resolving to an {@link ApiResponse}
     */
    CompletableFuture<ApiResponse> request(Method method, URI uri, Map<String, String> headers, String body);

    default RequestBuilder requestBuilder(Method method) {
        return new RequestBuilder(this, method);
    }

    enum Method {
        GET,
        PUT,
        POST,
        DELETE;

        boolean allowsBody() {
            return this == PUT || this == POST;
        }
    }

    class RequestBuilder {
        private final ApiClient apiClient;
        private final Method method;
        private final Map<String, String> headers = new HashMap<>();
        private URI uri;
        private String body;

        RequestBuilder(ApiClient apiClient, Method method) {
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

    class Config {
        private final SSLContext sslContext;
        private final Duration connectionTimeout;
        private final Duration requestTimeout;
        private final String userAgent;

        public Config(SSLContext sslContext,
                      Duration connectionTimeout,
                      Duration requestTimeout,
                      String userAgent) {
            this.sslContext = sslContext;
            this.connectionTimeout = connectionTimeout;
            this.requestTimeout = requestTimeout;
            this.userAgent = userAgent;
        }

        public SSLContext getSslContext() {
            return sslContext;
        }

        public Duration getConnectionTimeout() {
            return connectionTimeout;
        }

        public Duration getRequestTimeout() {
            return requestTimeout;
        }

        public String getUserAgent() {
            return userAgent;
        }
    }
}
