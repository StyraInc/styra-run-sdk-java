package com.styra.run;

import com.fasterxml.jackson.jr.ob.JSON;
import com.styra.run.Utils.Nullable;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Future;
import java.util.function.Predicate;

public class StyraRun {
    private static final Predicate<Result<?>> DEFAULT_CHECK_PREDICATE = (result) -> result.asSafeBoolean(false);

    private final ApiClient apiClient;
    private final Json json;
    private final URL baseUrl;
    private final Map<String, String> headers;

    private StyraRun(URL url, String token, ApiClient apiClient, Json json) {
        this.baseUrl = url;
        this.headers = Collections.singletonMap(
                "Authorization", String.format("Bearer %s", token));
        this.apiClient = apiClient;
        this.json = json;
    }

    public Future<Result<?>> query(String path) {
        return query(path, null);
    }

    public CompletableFuture<Result<?>> query(String path, Object input) {
        Objects.requireNonNull(path, "path must not be null");

        Map<String, ?> body = Nullable.map(input,
                (v) -> Collections.singletonMap("input", input),
                Collections.emptyMap());

        return makeUrl("data", path)
                .thenCombine(toJson(body), ApiRequest::new)
                .thenCompose((request) -> apiClient.post(request.url, request.body, headers))
                .thenApply((response) -> {
                    if (!response.isSuccessful()) {
                        throw new CompletionException(new StyraRunHttpException(
                                response.getStatusCode(), response.getBody(),
                                json.toOptionalMap(response.getBody())
                                        .map(ApiError::fromMap)
                                        .orElse(null)));
                    } else {
                        Map<String, ?> value = json.toOptionalMap(response.getBody())
                                .orElse(Collections.emptyMap());
                        return Result.fromResponseMap(value);
                    }
                });
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

    private CompletableFuture<String> toJson(Object value) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return json.from(value);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }, Runnable::run);
    }

    private CompletableFuture<URL> makeUrl(String... path) {
        return CompletableFuture.supplyAsync(() -> {

            try {
                return Utils.Url.appendPath(baseUrl, path);
            } catch (StyraRunException e) {
                throw new CompletionException(e);
            }
        }, Runnable::run);
    }

    public static Builder builder(String url, String token) {
        return new Builder(url, token);
    }

    public static final class Builder {
        private final String url;
        private final String token;
        private ApiClient apiClient;
        private Json json;

        public Builder(String url, String token) {
            this.url = Objects.requireNonNull(url, "url must not be null");
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
            URL typedUrl;
            try {
                typedUrl = new URL(url);
            } catch (MalformedURLException e) {
                throw new IllegalStateException("Malformed API URL", e);
            }

            ApiClient apiClient = Nullable.firstNonNull(
                    () -> this.apiClient, OkHttpApiClient::new);

            Json json = Nullable.firstNonNull(
                    () -> this.json, Json::new);

            return new StyraRun(typedUrl, token, apiClient, json);
        }
    }

    static class ApiRequest {
        final URL url;
        final String body;

        private ApiRequest(URL url, String body) {
            this.url = url;
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
