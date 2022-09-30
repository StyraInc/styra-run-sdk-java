package com.styra.run;

import com.styra.run.spi.ApiClientProvider;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class NoopApiClient implements ApiClient, ApiClientProvider {
    @Override
    public CompletableFuture<ApiResponse> get(URI uri, Map<String, String> headers) {
        return null;
    }

    @Override
    public CompletableFuture<ApiResponse> put(URI uri, String body, Map<String, String> headers) {
        return null;
    }

    @Override
    public CompletableFuture<ApiResponse> post(URI uri, String body, Map<String, String> headers) {
        return null;
    }

    @Override
    public CompletableFuture<ApiResponse> delete(URI uri, Map<String, String> headers) {
        return null;
    }

    @Override
    public CompletableFuture<ApiResponse> request(Method method, URI uri, Map<String, String> headers, String body) {
        return null;
    }

    @Override
    public ApiClient create() {
        return new NoopApiClient();
    }
}
