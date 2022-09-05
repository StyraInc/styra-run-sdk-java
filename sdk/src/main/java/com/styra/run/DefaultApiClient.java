package com.styra.run;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class DefaultApiClient implements ApiClient {
    private final HttpClient client = HttpClient.newHttpClient();

    @Override
    public CompletableFuture<ApiResponse> get(URI uri, Map<String, String> headers) {
        return sendAsync(requestBuilder(uri, headers)
                .GET()
                .build());
    }

    @Override
    public CompletableFuture<ApiResponse> put(URI uri, String body, Map<String, String> headers) {
        return sendAsync(requestBuilder(uri, headers)
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build());
    }

    @Override
    public CompletableFuture<ApiResponse> post(URI uri, String body, Map<String, String> headers) {
        return sendAsync(requestBuilder(uri, headers)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build());
    }

    @Override
    public CompletableFuture<ApiResponse> delete(URI uri, Map<String, String> headers) {
        return sendAsync(requestBuilder(uri, headers)
                .DELETE()
                .build());
    }

    private static HttpRequest.Builder requestBuilder(URI uri, Map<String, String> headers) {
        var requestBuilder = HttpRequest.newBuilder(uri);
        headers.forEach(requestBuilder::header);
        return requestBuilder;
    }

    private CompletableFuture<ApiResponse> sendAsync(HttpRequest request) {
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply((response) -> new ApiResponse(response.statusCode(), response.body()));
    }
}
