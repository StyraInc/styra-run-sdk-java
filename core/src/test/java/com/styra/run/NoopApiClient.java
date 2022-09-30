package com.styra.run;

import com.styra.run.spi.ApiClientProvider;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class NoopApiClient implements ApiClient, ApiClientProvider {
    @Override
    public CompletableFuture<ApiResponse> request(Method method, URI uri, Map<String, String> headers, String body) {
        return CompletableFuture.failedFuture(new Exception("Not implemented"));
    }

    @Override
    public ApiClient create() {
        return new NoopApiClient();
    }
}
