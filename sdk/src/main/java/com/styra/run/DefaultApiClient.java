package com.styra.run;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class DefaultApiClient implements ApiClient {
    private final Config config;
    private final HttpClient client;

    public DefaultApiClient(Config config) {
        this.config = config;
        client = HttpClient.newBuilder()
                .sslContext(config.getSslContext())
                .connectTimeout(config.getConnectionTimeout())
                .build();
    }

    @Override
    public CompletableFuture<ApiResponse> request(Method method, URI uri, Map<String, String> headers, String body) {
        var requestBuilder = HttpRequest.newBuilder(uri);
        headers.forEach(requestBuilder::header);
        switch (method) {
            case GET: requestBuilder.GET(); break;
            case PUT: requestBuilder.PUT(publisherFor(body)); break;
            case POST: requestBuilder.POST(publisherFor(body)); break;
            case DELETE: requestBuilder.DELETE(); break;
            default: return CompletableFuture.failedFuture(new StyraRunException(String.format("Unsupported method %s", method)));
        }

        var request = requestBuilder
                .timeout(config.getRequestTimeout())
                .setHeader("User-Agent", config.getUserAgent())
                .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply((response) -> new ApiResponse(response.statusCode(), response.body()));
    }

    private static HttpRequest.BodyPublisher publisherFor(String body) {
        if (body != null) {
            return HttpRequest.BodyPublishers.ofString(body);
        } else {
            return HttpRequest.BodyPublishers.noBody();
        }
    }
}
