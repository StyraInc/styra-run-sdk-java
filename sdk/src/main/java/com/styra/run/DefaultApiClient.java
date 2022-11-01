package com.styra.run;

import java.net.SocketException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.styra.run.utils.Futures.unwrapException;
import static java.util.concurrent.CompletableFuture.failedFuture;

public class DefaultApiClient implements ApiClient {
    public static final String REQ_TIMOUT_MSG = "request timed out";
    
    private final Config config;
    private final HttpClient client;

    public DefaultApiClient(Config config) {
        this(config, HttpClient.newBuilder()
                .sslContext(config.getSslContext())
                .connectTimeout(config.getConnectionTimeout())
                .build());
    }

    public DefaultApiClient(Config config, HttpClient client) {
        this.config = config;
        this.client = client;
    }

    @Override
    public CompletableFuture<ApiResponse> request(Method method, URI uri, Map<String, String> headers, String body) {
        var requestBuilder = HttpRequest.newBuilder(uri)
                .timeout(config.getRequestTimeout())
                .setHeader("User-Agent", config.getUserAgent());
        headers.forEach(requestBuilder::header);

        switch (method) {
            case GET:
                requestBuilder.GET();
                break;
            case PUT:
                requestBuilder.PUT(publisherFor(body));
                break;
            case POST:
                requestBuilder.POST(publisherFor(body));
                break;
            case DELETE:
                requestBuilder.DELETE();
                break;
            default:
                return failedFuture(new StyraRunException(String.format("Unsupported method %s", method)));
        }

        var request = requestBuilder.build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply((response) -> new ApiResponse(response.statusCode(), response.body()))
                .exceptionallyCompose(e -> {
                    Throwable unwrapped = unwrapException(e);
                    if (unwrapped instanceof SocketException ||
                            (unwrapped instanceof HttpTimeoutException && REQ_TIMOUT_MSG.equals(unwrapped.getMessage()))) {
                        return failedFuture(new RetryException(unwrapped));
                    }
                    return failedFuture(e);
                });
    }

    @Override
    public void close() {
    }

    private static HttpRequest.BodyPublisher publisherFor(String body) {
        if (body != null) {
            return HttpRequest.BodyPublishers.ofString(body);
        } else {
            return HttpRequest.BodyPublishers.noBody();
        }
    }
}
