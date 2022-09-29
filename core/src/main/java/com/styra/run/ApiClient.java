package com.styra.run;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface ApiClient {

    CompletableFuture<ApiResponse> get(URI uri, Map<String, String> headers);

    CompletableFuture<ApiResponse> put(URI uri, String body, Map<String, String> headers);

    CompletableFuture<ApiResponse> post(URI uri, String body, Map<String, String> headers);

    CompletableFuture<ApiResponse> delete(URI uri, Map<String, String> headers);
}
