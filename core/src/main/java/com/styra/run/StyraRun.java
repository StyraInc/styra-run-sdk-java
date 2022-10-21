package com.styra.run;

import com.styra.run.ApiClient.RequestBuilder;
import com.styra.run.discovery.ApiGatewaySelector;
import com.styra.run.discovery.Gateway;
import com.styra.run.discovery.GatewaySelectionStrategy;
import com.styra.run.discovery.GatewaySelector;
import com.styra.run.discovery.SimpleGatewaySelectionStrategy;
import com.styra.run.discovery.StaticGatewaySelector;
import com.styra.run.spi.ApiClientFactory;
import com.styra.run.utils.Futures;
import com.styra.run.utils.Null;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.styra.run.ApiClient.Method.DELETE;
import static com.styra.run.ApiClient.Method.GET;
import static com.styra.run.ApiClient.Method.POST;
import static com.styra.run.ApiClient.Method.PUT;
import static com.styra.run.utils.Futures.failedFuture;
import static com.styra.run.utils.Null.firstNonNull;
import static com.styra.run.utils.Null.orThrow;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.CompletableFuture.completedFuture;

public class StyraRun implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(StyraRun.class);

    private static final Predicate<Result<?>> DEFAULT_CHECK_PREDICATE = (result) -> result.getSafe(Boolean.class, false);

    private final ApiClient apiClient;
    private final Json json;
    private final GatewaySelector gatewaySelector;
    private final String token;
    private final int batchQueryItemsMax;

    private StyraRun(String token,
                     ApiClient apiClient,
                     Json json,
                     GatewaySelector gatewaySelector, int batchQueryItemsMax) {
        this.token = token;
        this.apiClient = apiClient;
        this.json = json;
        this.gatewaySelector = gatewaySelector;
        this.batchQueryItemsMax = batchQueryItemsMax;
    }

    Json getJson() {
        return json;
    }

    ApiClient getApiClient() {
        return apiClient;
    }

    public CompletableFuture<Result<?>> query(String path) {
        return query(path, null);
    }

    public CompletableFuture<Result<?>> query(String path, Input<?> input) {
        requireNonNull(path, "path must not be null");

        return completedFuture(apiClient.requestBuilder(POST)
                .headers(getCommonHeaders())
                .jsonContentType())
                .thenCombine(serializeBody(new InputContainer(input)), RequestBuilder::body)
                .thenCompose((request) -> gatewaySelector.retry(request, "data", path))
                .thenApply(this::handleResponse)
                .thenApply(Result::fromResponseMap)
                .thenApply((result) -> {
                    logger.debug("Query: path='{}'; input={}; result={}", path, input, result);
                    return result;
                });
    }

    public BatchQueryBuilder batchQueryBuilder() {
        return new BatchQueryBuilder();
    }

    public CompletableFuture<ListResult> batchQuery(List<BatchQuery.Item> items) {
        return batchQuery(items, null);
    }

    public CompletableFuture<ListResult> batchQuery(List<BatchQuery.Item> items, Input<?> globalInput) {
        requireNonNull(items, "items must not be null");
        if (items.isEmpty()) {
            throw new IllegalArgumentException("items must not be empty");
        }

        List<BatchQuery> chunks = new BatchQuery(items, globalInput)
                .chunk(batchQueryItemsMax);

        List<CompletableFuture<ListResult>> futures = chunks.stream()
                .map(this::batchQuery)
                .collect(Collectors.toList());

        return Futures.allOf(futures)
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
                    logger.debug("Batch query: items='{}'; input={}; result={}", items, globalInput, result);
                    return result;
                });
    }

    private CompletableFuture<ListResult> batchQuery(BatchQuery query) {
        return completedFuture(apiClient.requestBuilder(POST)
                .headers(getCommonHeaders())
                .jsonContentType())
                .thenCombine(serializeBody(query), RequestBuilder::body)
                .thenCompose(request -> gatewaySelector.retry(request, "data_batch"))
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
        requireNonNull(path, "path must not be null");

        return completedFuture(apiClient.requestBuilder(GET)
                .headers(getCommonHeaders()))
                .thenCompose(request -> gatewaySelector.retry(request, "data", path))
                .thenApply((response) -> {
                    if (response.isNotFoundStatus()) {
                        return new Result<>(defaultSupplier.get());
                    } else {
                        return Result.fromResponseMap(handleResponse(response));
                    }
                })
                .thenApply((result) -> {
                    logger.debug("GET data: path='{}'; result={}", path, result);
                    return result;
                });
    }

    public CompletableFuture<Result<Void>> putData(String path, Object data) {
        requireNonNull(path, "path must not be null");
        requireNonNull(data, "data must not be null");

        return completedFuture(apiClient.requestBuilder(PUT)
                .headers(getCommonHeaders())
                .jsonContentType())
                .thenCombine(toJson(data), RequestBuilder::body)
                .thenCompose(request -> gatewaySelector.retry(request, "data", path))
                .thenApply(this::handleResponse)
                .thenApply(Result::empty)
                .thenApply((result) -> {
                    logger.debug("PUT data: path='{}'; data='{}'; result={}", path, data, result);
                    return result;
                });
    }

    public CompletableFuture<Result<Void>> deleteData(String path) {
        requireNonNull(path, "path must not be null");

        return completedFuture(apiClient.requestBuilder(DELETE)
                .headers(getCommonHeaders()))
                .thenCompose(request -> gatewaySelector.retry(request, "data", path))
                .thenApply(this::handleResponse)
                .thenApply(Result::empty)
                .thenApply((result) -> {
                    logger.debug("DELETE data: path='{}'; result={}", path, result);
                    return result;
                });
    }

    @Override
    public void close() throws Exception {
        apiClient.close();
    }

    private CompletableFuture<String> serializeBody(SerializableAsMap body) {
        try {
            return completedFuture(getJson().from(Null.map(body,
                    SerializableAsMap::toMap,
                    Collections.emptyMap())));
        } catch (IOException e) {
            return failedFuture(new StyraRunException("Input could not be serialized into json", e));
        }
    }

    private Map<String, ?> handleResponse(ApiResponse response) {
        return json.toOptionalMap(handleRawResponse(response))
                .orElse(Collections.emptyMap());
    }

    private String handleRawResponse(ApiResponse response) {
        if (!response.isSuccessful()) {
            throw new CompletionException(new StyraRunHttpException(
                    response.getStatusCode(), response.getBody(),
                    ApiError.fromApiResponse(response, json)));
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

    public static Builder builder(String url, String token) {
        return new Builder(url, token);
    }

    public static Builder builder(List<String> gateways, String token) {
        return new Builder(gateways, token);
    }

    private Map<String, String> getCommonHeaders() {
        return makeAuthorizationHeader(token);
    }

    private static Map<String, String> makeAuthorizationHeader(String token) {
        return Collections.singletonMap("Authorization", String.format("Bearer %s", token));
    }

    public class BatchQueryBuilder {
        private final List<BatchQuery.Item> items = new LinkedList<>();
        private Input<?> input;

        private BatchQueryBuilder() {
        }

        public BatchQueryBuilder input(Input<?> input) {
            this.input = input;
            return this;
        }

        public BatchQueryBuilder query(String path) {
            return query(path, null);
        }

        public BatchQueryBuilder query(String path, Input<?> input) {
            requireNonNull(path, "path must not be null");
            items.add(new BatchQuery.Item(path, input));
            return this;
        }

        public CompletableFuture<ListResult> execute() {
            return batchQuery(items, input);
        }
    }

    public static final class Builder {
        private final String envUri;
        private final List<String> gateways;
        private final String token;
        private ApiClientFactory apiClientFactory;
        private GatewaySelectionStrategy.Factory gatewaySelectionStrategyFactory = new SimpleGatewaySelectionStrategy.Factory();
        private Json json;
        private int batchQueryItemsMax = 20;
        private int maxRetryAttempts = 3;
        private SSLContext sslContext;
        private Duration connectionTimeout = Duration.ofSeconds(1);
        private Duration requestTimeout = Duration.ofSeconds(3);
        private String userAgent = String.format("Styra Run Java Client (%s)",
                firstNonNull(getClass().getPackage().getImplementationVersion(), "DEVELOPMENT"));

        public Builder(String envUri, String token) {
            this.envUri = orThrow(envUri, "url must not be null");
            this.gateways = null;
            this.token = orThrow(token, "token must not be null");
        }

        public Builder(List<String> gateways, String token) {
            this.envUri = null;
            this.gateways = orThrow(gateways, "gateways must not be null");
            this.token = orThrow(token, "token must not be null");
        }

        public Builder apiClientFactory(ApiClientFactory factory) {
            this.apiClientFactory = orThrow(factory, "factory must not be null");
            return this;
        }

        public Builder json(Json json) {
            this.json = orThrow(json, "json must not be null");
            return this;
        }

        public Builder batchQueryItemsMax(int max) {
            if (max < 0) {
                throw new IllegalArgumentException("max must not be negative");
            }
            this.batchQueryItemsMax = max;
            return this;
        }

        public Builder maxRetryAttempts(int max) {
            if (max <= 0) {
                throw new IllegalArgumentException("max must not be zero or negative");
            }
            this.maxRetryAttempts = max;
            return this;
        }

        public Builder gatewaySelectionStrategy(GatewaySelectionStrategy.Factory factory) {
            this.gatewaySelectionStrategyFactory = orThrow(factory, "factory must not be null");
            return this;
        }

        public Builder sslContext(SSLContext sslContext) {
            this.sslContext = orThrow(sslContext, "sslContext must not be null");
            return this;
        }

        public Builder connectionTimeout(Duration timeout) {
            this.connectionTimeout = orThrow(timeout, "timeout must not be null");
            return this;
        }

        public Builder requestTimeout(Duration timeout) {
            this.requestTimeout = orThrow(timeout, "timeout must not be null");
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = orThrow(userAgent, "userAgent must not be null");
            return this;
        }

        // TODO: Throw StyraRunException
        public StyraRun build() {
            SSLContext sslContext;
            if (this.sslContext != null) {
                sslContext = this.sslContext;
            } else {
                try {
                    sslContext = SSLContext.getDefault();
                } catch (NoSuchAlgorithmException e) {
                    throw new IllegalStateException("Failed to get default SSL Context", e);
                }
            }
            ApiClient.Config clientConfig = new ApiClient.Config(sslContext, connectionTimeout, requestTimeout, userAgent);
            ApiClient apiClient;
            if (apiClientFactory != null) {
                apiClient = apiClientFactory.create(clientConfig);
            } else {
                apiClient = ApiClientLoader.loadDefaultClient().create(clientConfig);
            }

            Json json = firstNonNull(
                    () -> this.json, Json::new);

            GatewaySelector gatewaySelector;
            if (envUri != null) {
                try {
                    Supplier<Map<String, String>> headerSupplier = () -> makeAuthorizationHeader(token);
                    gatewaySelector = new ApiGatewaySelector(gatewaySelectionStrategyFactory, maxRetryAttempts,
                            apiClient, json, new URI(envUri), headerSupplier);
                } catch (URISyntaxException e) {
                    throw new IllegalStateException(String.format("Malformed environment URI: %s", envUri), e);
                }
            } else if (gateways != null) {
                List<Gateway> list = new ArrayList<>();
                for (String gateway : gateways) {
                    try {
                        list.add(new Gateway(new URI(gateway)));
                    } catch (URISyntaxException e) {
                        throw new IllegalStateException(String.format("Malformed gateway URI: %s", gateway), e);
                    }
                }
                gatewaySelector = new StaticGatewaySelector(gatewaySelectionStrategyFactory, maxRetryAttempts, list);
            } else {
                throw new IllegalStateException("Environment URI or gateway list must be set");
            }

            return new StyraRun(token, apiClient,
                    json, gatewaySelector, batchQueryItemsMax);
        }
    }
}

