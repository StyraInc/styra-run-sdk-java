package com.styra.run;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface ApiClient {
    // TODO: package request params into ApiRequest object?
    CompletableFuture<ApiResponse> request(Method method, URI uri, Map<String, String> headers, String body);

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
