package com.styra.run.discovery;

import com.styra.run.ApiClient.RequestBuilder;
import com.styra.run.ApiResponse;
import com.styra.run.RetryException;
import com.styra.run.StyraRunException;
import com.styra.run.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.styra.run.Utils.Futures.async;
import static com.styra.run.Utils.Futures.failedFuture;
import static com.styra.run.Utils.Url.appendPath;
import static java.util.concurrent.CompletableFuture.completedFuture;

public abstract class GatewaySelector {
    private static final Logger logger = LoggerFactory.getLogger(GatewaySelector.class);
    private static final List<Integer> STATUS_CODES_TO_RETRY = Arrays.asList(421, 502, 503, 504);

    private final Object gatewaysFetchLock = new Object();

    private final GatewaySelectionStrategy.Factory gatewaySelectionStrategyFactory;
    private volatile GatewaySelectionStrategy discoveryStrategy;
    private final int maxAttempts;

    public GatewaySelector(GatewaySelectionStrategy.Factory gatewaySelectionStrategyFactory, int maxAttempts) {
        this.gatewaySelectionStrategyFactory = gatewaySelectionStrategyFactory;
        this.maxAttempts = maxAttempts;
    }

    public CompletableFuture<ApiResponse> retry(RequestBuilder request, String... path) {
        return retry(gateway -> async(() -> appendPath(gateway.getUri(), path))
                .thenApply(request::uri)
                .thenCompose(RequestBuilder::request));
    }

    private CompletableFuture<ApiResponse> retry(Function<Gateway, CompletableFuture<ApiResponse>> request) {
        return getGatewaySelectionStrategy()
                .thenCompose((strategy) -> retry(request, strategy, strategy.current(), 1,
                        () -> failedFuture(new StyraRunException("No API request attempts allowed"))));
    }

    // TODO: add logging
    private CompletableFuture<ApiResponse> retry(Function<Gateway, CompletableFuture<ApiResponse>> request,
                                                 GatewaySelectionStrategy strategy,
                                                 Gateway gateway,
                                                 int attempt,
                                                 Supplier<CompletableFuture<ApiResponse>> onTooManyAttempts) {
        if (gateway == null || attempt > maxAttempts || attempt > strategy.size()) {
            return onTooManyAttempts.get();
        }

        return request.apply(gateway)
                .thenCompose((response) -> {
                    if (STATUS_CODES_TO_RETRY.contains(response.getStatusCode())) {
                        return retry(request, strategy, strategy.nextIfMatch(gateway), attempt + 1,
                                () -> completedFuture(response));
                    }
                    return completedFuture(response);
                })
                .exceptionallyCompose((e) -> {
                    Throwable unwrapped = Utils.Futures.unwrapCompletionException(e);
                    if (unwrapped instanceof RetryException) {
                        return retry(request, strategy, strategy.nextIfMatch(gateway), attempt + 1,
                                () -> failedFuture(unwrapped.getCause()));
                    }
                    throw new CompletionException(unwrapped);
                });
    }

    protected abstract List<Gateway> fetchGateways();

    private CompletableFuture<GatewaySelectionStrategy> getGatewaySelectionStrategy() {
        // We fetch gateways synchronously, as there is no point in retrieving them more than once,
        // and no concurrent work can be done until they're resolved.
        if (discoveryStrategy == null) {
            synchronized (gatewaysFetchLock) {
                if (discoveryStrategy == null) {
                    logger.trace("Fetching gateways");
                    List<Gateway> gateways = fetchGateways();
                    if (gateways.isEmpty()) {
                        return failedFuture(new StyraRunException("No gateways could be fetched"));
                    }
                    logger.debug("Gateways: {}", gateways.stream()
                            .map((g) -> g.getUri().toString())
                            .collect(Collectors.joining(", ")));
                    discoveryStrategy = gatewaySelectionStrategyFactory.create(gateways);
                }
            }
        }
        return completedFuture(discoveryStrategy);
    }
}
