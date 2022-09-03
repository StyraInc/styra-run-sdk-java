package com.styra.run;

import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface ApiClient {
    class ApiResponse {
        private final int statusCode;
        private final String body;

        public ApiResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        public boolean isSuccessful() {
            // Not considering 3XX as successful
            return statusCode >= 200 && statusCode < 300;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getBody() {
            return body;
        }
    }

    CompletableFuture<ApiResponse> get(URL url, Map<String, String> headers);

    CompletableFuture<ApiResponse> put(URL url, String body, Map<String, String> headers);

    CompletableFuture<ApiResponse> post(URL url, String body, Map<String, String> headers);

    CompletableFuture<ApiResponse> delete(URL url, Map<String, String> headers);
}

// TODO: Break out into separate support library
class OkHttpApiClient implements ApiClient {
    private final OkHttpClient client = new OkHttpClient();

    OkHttpApiClient() {
    }

    @Override
    public CompletableFuture<ApiResponse> get(URL url, Map<String, String> headers) {
        return request("GET", url, null, headers);
    }

    @Override
    public CompletableFuture<ApiResponse> put(URL url, String body, Map<String, String> headers) {
        return request("PUT", url, body, headers);
    }

    @Override
    public CompletableFuture<ApiResponse> post(URL url, String body, Map<String, String> headers) {
        return request("POST", url, body, headers);
    }

    @Override
    public CompletableFuture<ApiResponse> delete(URL url, Map<String, String> headers) {
        return request("DELETE", url, null, headers);
    }

    public CompletableFuture<ApiResponse> request(String method, URL url, String body, Map<String, String> headers) {
        Request request = new Request.Builder()
                .url(url)
                .headers(Headers.of(headers))
                .method(method, body != null ? RequestBody.create(body, MediaType.get("application/json")) : null)
                .build();

        return handleResponse(client.newCall(request));
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
