package com.styra.run;

import com.styra.run.Utils.Null;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Future;
import java.util.function.Predicate;
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

    public Future<Result<?>> query(String path) {
        return query(path, null);
    }

    public CompletableFuture<Result<?>> query(String path, Object input) {
        Objects.requireNonNull(path, "path must not be null");

        HeadersBuilder headers = makeHeadersBuilder().json();
        Map<String, ?> body = Null.map(input,
                (v) -> Collections.singletonMap("input", input),
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

    public CompletableFuture<ListResult> batchQuery(List<Query> items, Object input) {
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
        return toJson(query)
                .thenCompose((json) -> apiClient.post(batchUri, json, headers.toMap()))
                .thenApply(this::handleResponse)
                .thenApply(ListResult::fromResponseMap);
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
        return json.toOptionalMap(handleRawResponse(response))
                .orElse(Collections.emptyMap());
    }

    private String handleRawResponse(ApiClient.ApiResponse response) {
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
        private String path;
        private Object input;

        Query() {
            this.path = null;
            this.input = null;
        }

        public Query(String path, Object input) {
            this.path = path;
            this.input = input;
        }

        public Object getInput() {
            return input;
        }

        public void setInput(Object input) {
            this.input = input;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
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
    }

    static class BatchQuery {
        private List<Query> items;
        private Object input;

        BatchQuery() {
            items = null;
            input = null;
        }

        public BatchQuery(List<Query> items, Object input) {
            this.items = items;
            this.input = input;
        }

        public Object getInput() {
            return input;
        }

        public void setInput(Object input) {
            this.input = input;
        }

        public List<Query> getItems() {
            return items;
        }

        public void setItems(List<Query> items) {
            this.items = items;
        }

        List<BatchQuery> chunk(int chunkSize) {
            return Utils.Collections.chunk(items, chunkSize).stream()
                    .map((items) -> new BatchQuery(items, input))
                    .collect(Collectors.toList());
        }
    }

    public class BatchQueryBuilder {
        private final List<Query> items = new LinkedList<>();
        private Object input;

        private BatchQueryBuilder() {
        }

        public BatchQueryBuilder input(Object input) {
            this.input = input;
            return this;
        }

        public BatchQueryBuilder addQuery(String path) {
            return addQuery(path, input);
        }

        public BatchQueryBuilder addQuery(String path, Object input) {
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

