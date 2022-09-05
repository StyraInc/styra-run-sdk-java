package com.styra.run;

import com.fasterxml.jackson.jr.ob.JSON;
import com.styra.run.Utils.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Future;
import java.util.function.Predicate;

public class StyraRun {
    private static final Predicate<Result<?>> DEFAULT_CHECK_PREDICATE = (result) -> result.asSafeBoolean(false);

    private final ApiClient apiClient;
    private final Json json;
    private final URI baseUri;
    private final String token;

    private StyraRun(URI uri, String token, ApiClient apiClient, Json json) {
        this.baseUri = uri;
        this.token = token;
        this.apiClient = apiClient;
        this.json = json;
    }

    private class HeadersBuilder {
        private final Map<String, String> headers = new HashMap<>();

        HeadersBuilder authorization(String token) {
            headers.put("Authorization", String.format("Bearer %s", token));
            return this;
        }

        HeadersBuilder contentType(String contentType) {
            headers.put("Content-Type", contentType);
            return this;
        }

        HeadersBuilder json() {
            return contentType("application/json");
        }

        Map<String, String> toMap() {
            return Collections.unmodifiableMap(headers);
        }
    }

    private HeadersBuilder makeHeadersBuilder() {
        return new HeadersBuilder().authorization(token);
    }

    public Future<Result<?>> query(String path) {
        return query(path, null);
    }

    public CompletableFuture<Result<?>> query(String path, Object input) {
        Objects.requireNonNull(path, "path must not be null");

        HeadersBuilder headers = makeHeadersBuilder().json();
        Map<String, ?> body = Nullable.map(input,
                (v) -> Collections.singletonMap("input", input),
                Collections.emptyMap());

        return makeUri("data", path)
                .thenCombine(toJson(body), ApiRequest::new)
                .thenCompose((request) -> apiClient.post(request.uri, request.body, headers.toMap()))
                .thenApply(this::handleResponse)
                .thenApply(Result::fromResponseMap);
    }

    public CompletableFuture<Boolean> check(String path) {
        return check(path, null, DEFAULT_CHECK_PREDICATE);
    }

    public CompletableFuture<Boolean> check(String path, Object input) {
        return check(path, input, DEFAULT_CHECK_PREDICATE);
    }

    public CompletableFuture<Boolean> check(String path, Predicate<Result<?>> predicate) {
        return check(path, null, predicate);
    }

    public CompletableFuture<Boolean> check(String path, Object input, Predicate<Result<?>> predicate) {
        return query(path, input).thenApply((predicate::test));
    }

    public CompletableFuture<Result<?>> getData(String path) {
        Objects.requireNonNull(path, "path must not be null");

        HeadersBuilder headers = makeHeadersBuilder();
        return makeUri("data", path)
                .thenCompose((url) -> apiClient.get(url, headers.toMap()))
                .thenApply(this::handleResponse)
                .thenApply(Result::fromResponseMap);
    }

    public CompletableFuture<Result<Void>> putData(String path, Object data) {
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(data, "data must not be null");

        HeadersBuilder headers = makeHeadersBuilder().json();
        return makeUri("data", path)
                .thenCombine(toJson(data), ApiRequest::new)
                .thenCompose((request) -> apiClient.put(request.uri, request.body, headers.toMap()))
                .thenApply(this::handleResponse)
                .thenApply(Result::empty);
    }

    public CompletableFuture<Result<Void>> deleteData(String path) {
        Objects.requireNonNull(path, "path must not be null");

        HeadersBuilder headers = makeHeadersBuilder().json();
        return makeUri("data", path)
                .thenCompose((url) -> apiClient.delete(url, headers.toMap()))
                .thenApply(this::handleResponse)
                .thenApply(Result::empty);
    }

    private Map<String, ?> handleResponse(ApiClient.ApiResponse response) {
        if (!response.isSuccessful()) {
            throw new CompletionException(new StyraRunHttpException(
                    response.getStatusCode(), response.getBody(),
                    json.toOptionalMap(response.getBody())
                            .map(ApiError::fromMap)
                            .orElse(null)));
        } else {
            return json.toOptionalMap(response.getBody())
                    .orElse(Collections.emptyMap());
        }
    }

    private CompletableFuture<String> toJson(Object value) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return json.from(value);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }, Runnable::run);
    }

    private CompletableFuture<URI> makeUri(String... path) {
        CompletableFuture<URI> future = new CompletableFuture<>();
        try {
            future.complete(Utils.Url.appendPath(baseUri, path));
        } catch (StyraRunException e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    public static Builder builder(String url, String token) {
        return new Builder(url, token);
    }

    public static final class Builder {
        private final String uri;
        private final String token;
        private ApiClient apiClient;
        private Json json;

        public Builder(String url, String token) {
            this.uri = Objects.requireNonNull(url, "url must not be null");
            this.token = Objects.requireNonNull(token, "token must not be null");
        }

        public Builder apiClient(ApiClient apiClient) {
            this.apiClient = apiClient;
            return this;
        }

        public Builder json(Json json) {
            this.json = json;
            return this;
        }

        public StyraRun build() {
            URI typedUri;
            try {
                typedUri = new URI(uri);
            } catch (URISyntaxException e) {
                throw new IllegalStateException("Malformed API URI", e);
            }

            ApiClient apiClient = Nullable.firstNonNull(
                    () -> this.apiClient, ApiClientLoader.loadDefaultClient()::create);

            Json json = Nullable.firstNonNull(
                    () -> this.json, Json::new);

            return new StyraRun(typedUri, token, apiClient, json);
        }
    }

    static class ApiRequest {
        final URI uri;
        final String body;

        private ApiRequest(URI uri, String body) {
            this.uri = uri;
            this.body = body;
        }
    }
}

// TODO: Make pluggable
class Json {
    String from(Object value) throws IOException {
        return JSON.std.asString(value);
    }

    Map<String, ?> toMap(String str) throws IOException {
        if (str == null) {
            return null;
        }

        return JSON.std.mapFrom(str);
    }

    Optional<Map<String, ?>> toOptionalMap(String str) {
        if (str == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(JSON.std.mapFrom(str));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    Object toAny(String str) {
        if (str == null) {
            return null;
        }

        try {
            return JSON.std.anyFrom(str);
        } catch (IOException e) {
            return str;
        }
    }
}
