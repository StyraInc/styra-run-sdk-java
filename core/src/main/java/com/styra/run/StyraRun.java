package com.styra.run;

import com.styra.run.ApiClient.ApiResponse;
import com.styra.run.Utils.Null;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class StyraRun {
    private static final Logger logger = LoggerFactory.getLogger(StyraRun.class);

    private static final Predicate<Result<?>> DEFAULT_CHECK_PREDICATE = (result) -> result.asSafeBoolean(false);

    private final ApiClient apiClient;
    private final Json json;
    private final URI baseUri;
    private final URI batchUri;
    private final String token;
    private final int batchQueryItemsMax;

    private StyraRun(URI baseUri,
                     URI batchUri,
                     String token,
                     ApiClient apiClient,
                     Json json,
                     int batchQueryItemsMax) {
        this.baseUri = baseUri;
        this.token = token;
        this.apiClient = apiClient;
        this.json = json;
        this.batchUri = batchUri;
        this.batchQueryItemsMax = batchQueryItemsMax;
    }

    Json getJson() {
        return json;
    }

    public CompletableFuture<Result<?>> query(String path) {
        return query(path, null);
    }

    public CompletableFuture<Result<?>> query(String path, Input<?> input) {
        Objects.requireNonNull(path, "path must not be null");

        HeadersBuilder headers = makeHeadersBuilder().json();
        Map<String, ?> body = Null.map(input,
                Input::toMap,
                Collections.emptyMap());

        return makeUri("data", path)
                .thenCombine(toJson(body), ApiRequest::new)
                .thenCompose((request) -> apiClient.post(request.uri, request.body, headers.toMap()))
                .thenApply(this::handleResponse)
                .thenApply(Result::fromResponseMap)
                .thenApply((result) -> {
                    logger.debug("Query: path='{}'; input={}; result={}", path, input, result);
                    return result;
                });
    }

    public BatchQueryBuilder buildBatchQuery() {
        return new BatchQueryBuilder();
    }

    public CompletableFuture<ListResult> batchQuery(List<Query> items, Input<?> input) {
        Objects.requireNonNull(items, "items must not be null");

        List<BatchQuery> chunks = new BatchQuery(items, input)
                .chunk(batchQueryItemsMax);

        List<CompletableFuture<ListResult>> futures = chunks.stream()
                .map(this::batchQuery)
                .collect(Collectors.toList());

        return Utils.Futures.allOf(futures)
                .thenApply((resultList) -> resultList.stream()
                        .reduce(ListResult::append)
                        .orElse(ListResult.empty()))
                .thenApply((result) -> {
                    if (result.size() != items.size()) {
                        throw new CompletionException(new StyraRunException(String.format(
                                "Number of items in batch query response (%d) does not match number of items in request (%d)",
                                result.size(), items.size())));
                    }
                    return result;
                })
                .thenApply((result) -> {
                    logger.debug("Batch query: items='{}'; input={}; result={}", items, input, result);
                    return result;
                });
    }

    private CompletableFuture<ListResult> batchQuery(BatchQuery query) {
        HeadersBuilder headers = makeHeadersBuilder().json();
        return toJson(query.toMap())
                .thenCompose((json) -> apiClient.post(batchUri, json, headers.toMap()))
                .thenApply(this::handleResponse)
                .thenApply(ListResult::fromResponseMap);
    }

    public CompletableFuture<Boolean> check(String path) {
        return check(path, null, DEFAULT_CHECK_PREDICATE);
    }

    public CompletableFuture<Boolean> check(String path, Input<?> input) {
        return check(path, input, DEFAULT_CHECK_PREDICATE);
    }

    public CompletableFuture<Boolean> check(String path, Predicate<Result<?>> predicate) {
        return check(path, null, predicate);
    }

    public CompletableFuture<Boolean> check(String path, Input<?> input, Predicate<Result<?>> predicate) {
        return query(path, input)
                .thenApply((predicate::test))
                .thenApply((allowed) -> {
                    logger.debug("Check: path='{}'; input={}; allowed={}", path, input, allowed);
                    return allowed;
                });
    }

    public CompletableFuture<Result<?>> getData(String path) {
        return getData(path, () -> null);
    }

    public CompletableFuture<Result<?>> getData(String path, Object def) {
        return getData(path, () -> def);
    }

    public CompletableFuture<Result<?>> getData(String path, Supplier<?> defaultSupplier) {
        Objects.requireNonNull(path, "path must not be null");

        HeadersBuilder headers = makeHeadersBuilder();
        return makeUri("data", path)
                .thenCompose((url) -> apiClient.get(url, headers.toMap()))
                .thenApply((response) -> {
                    if (response.getStatusCode() == 404) {
                        return new Result<>(defaultSupplier.get());
                    } else {
                        return Result.fromResponseMap(handleResponse(response));
                    }
                });
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

    private Map<String, ?> handleResponse(ApiResponse response) {
        return json.toOptionalMap(handleRawResponse(response))
                .orElse(Collections.emptyMap());
    }

    private String handleRawResponse(ApiResponse response) {
        if (!response.isSuccessful()) {
            throw new CompletionException(new StyraRunHttpException(
                    response.getStatusCode(), response.getBody(),
                    json.toOptionalMap(response.getBody())
                            .map(ApiError::fromMap)
                            .orElse(null)));
        } else {
            return response.getBody();
        }
    }

    private CompletableFuture<String> toJson(Object value) {
        CompletableFuture<String> future = new CompletableFuture<>();
        try {
            future.complete(json.from(value));
        } catch (IOException e) {
            future.completeExceptionally(e);
        }
        return future;
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

    public static class Query {
        private final String path;
        private final Input<?> input;

        public Query(String path, Input<?> input) {
            Objects.requireNonNull(path, "path must not be null");
            this.path = path;
            this.input = input;
        }

        public static Query fromMap(Map<?, ?> map) {
            String path = (String) map.get("path");
            Input<?> input = Utils.Null.map(map.get("input"), Input::new);
            return new Query(path, input);
        }

        public Input<?> getInput() {
            return input;
        }

        public String getPath() {
            return path;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Query{")
                    .append("path='").append(path).append('\'');

            if (input != null) {
                sb.append(", input=")
                        .append(input);
            }

            sb.append('}');
            return sb.toString();
        }

        public Map<String, ?> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("path", path);
            if (input != null) {
                map.putAll(input.toMap());
            }
            return map;
        }
    }

    static class BatchQuery {
        private final List<Query> items;
        private final Input<?> input;

        public BatchQuery(List<Query> items, Input<?> input) {
            Objects.requireNonNull(items, "items must not be null");

            this.items = items;
            this.input = input;
        }

        public static BatchQuery fromMap(Map<?, ?> map) {
            List<Query> items = ((List<?>) map.get("items")).stream()
                    .map((item) -> Query.fromMap((Map<?, ?>) item))
                    .collect(Collectors.toList());
            Input<?> input = Utils.Null.map(map.get("input"), Input::new);
            return new BatchQuery(items, input);
        }

        public Input<?> getInput() {
            return input;
        }

        public List<Query> getItems() {
            return items;
        }

        List<BatchQuery> chunk(int chunkSize) {
            return Utils.Collections.chunk(items, chunkSize).stream()
                    .map((items) -> new BatchQuery(items, input))
                    .collect(Collectors.toList());
        }

        public Map<String, ?> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("items", items.stream()
                    .map(Query::toMap)
                    .collect(Collectors.toList()));
            if (input != null) {
                map.putAll(input.toMap());
            }
            return map;
        }
    }

    public class BatchQueryBuilder {
        private final List<Query> items = new LinkedList<>();
        private Input<?> input;

        private BatchQueryBuilder() {
        }

        public BatchQueryBuilder input(Input<?> input) {
            this.input = input;
            return this;
        }

        public BatchQueryBuilder addQuery(String path) {
            return addQuery(path, input);
        }

        public BatchQueryBuilder addQuery(String path, Input<?> input) {
            Objects.requireNonNull(path, "path must not be null");
            items.add(new Query(path, input));
            return this;
        }

        public CompletableFuture<ListResult> query() {
            return batchQuery(items, input);
        }
    }

    public static Builder builder(String url, String token) {
        return new Builder(url, token);
    }

    public static final class Builder {
        private final String uri;
        private final String token;
        private ApiClient apiClient;
        private Json json;
        private int batchQueryItemsMax = 20;

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

        public Builder batchQueryItemsMax(int max) {
            if (max < 0) {
                throw new IllegalArgumentException("max must not be negative");
            }
            this.batchQueryItemsMax = max;
            return this;
        }

        public StyraRun build() {
            URI baseUri;
            URI batchUri;
            try {
                baseUri = new URI(uri);
                batchUri = Utils.Url.appendPath(baseUri, "data_batch");
            } catch (URISyntaxException | StyraRunException e) {
                throw new IllegalStateException("Malformed API URI", e);
            }

            ApiClient apiClient = Null.firstNonNull(
                    () -> this.apiClient, ApiClientLoader.loadDefaultClient()::create);

            Json json = Null.firstNonNull(
                    () -> this.json, Json::new);

            return new StyraRun(baseUri, batchUri, token, new LoggingApiClient(apiClient),
                    json, batchQueryItemsMax);
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

    private static class HeadersBuilder {
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
}

