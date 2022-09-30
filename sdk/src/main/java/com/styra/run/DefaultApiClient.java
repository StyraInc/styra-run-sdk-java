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
    public CompletableFuture<ApiResponse> request(Method method, URI uri, Map<String, String> headers, String body) {
        var requestBuilder = HttpRequest.newBuilder(uri);
        headers.forEach(requestBuilder::header);
        switch (method) {
            case GET: requestBuilder.GET(); break;
            case PUT: requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(body)); break;
            case POST: requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body)); break;
            case DELETE: requestBuilder.DELETE(); break;
            default: return CompletableFuture.failedFuture(new StyraRunException(String.format("Unsupported method %s", method)));
        }

        return client.sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
                .thenApply((response) -> new ApiResponse(response.statusCode(), response.body()));
    }
}
