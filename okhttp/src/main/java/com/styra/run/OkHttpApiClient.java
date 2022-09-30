package com.styra.run;

import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

class OkHttpApiClient implements ApiClient {
    private final OkHttpClient client = new OkHttpClient();

    public OkHttpApiClient() {
    }

    @Override
    public CompletableFuture<ApiResponse> request(Method method, URI uri, Map<String, String> headers, String body) {
        return toUrl(uri).thenCompose((url) -> {
            Request request = new Request.Builder()
                    .url(url)
                    .headers(Headers.of(headers))
                    .method(method.name(), body != null ? RequestBody.create(body, MediaType.get("application/json")) : null)
                    .build();
            return handleResponse(client.newCall(request));
        });
    }

    private static CompletableFuture<URL> toUrl(URI uri) {
        CompletableFuture<URL> future = new CompletableFuture<>();
        try {
            future.complete(uri.toURL());
        } catch (MalformedURLException e) {
            future.completeExceptionally(new StyraRunException("Failed to construct URL for API call", e));
        }
        return future;
    }

    private static CompletableFuture<ApiResponse> handleResponse(Call call) {
        CompletableFuture<ApiResponse> future = new CompletableFuture<>();
        call.enqueue(new Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                try {
                    future.complete(new ApiResponse(response.code(), getBody(response)));
                } catch (IOException e) {
                    future.completeExceptionally(new StyraRunException("Failed to send request to Styra Run API", e));
                }
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                future.completeExceptionally(new StyraRunException("Failed to send request to Styra Run API", e));
            }
        });
        return future;
    }

    private static String getBody(Response response) throws IOException {
        ResponseBody responseBody = response.body();
        if (responseBody == null) {
            return null;
        }
        return responseBody.string();
    }
}
