package com.styra.run.discovery;

import com.styra.run.ApiResponse;
import com.styra.run.RetryException;
import com.styra.run.StyraRunException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    public CompletableFuture<ApiResponse> retry(Function<Gateway, CompletableFuture<ApiResponse>> request) {
        return getGatewaySelectionStrategy()
                .thenCompose((strategy) -> retry(request, strategy, strategy.current(), 1));
    }

    private CompletableFuture<ApiResponse> retry(Function<Gateway, CompletableFuture<ApiResponse>> request, GatewaySelectionStrategy strategy, Gateway gateway, int attempt) {
        if (attempt > maxAttempts || attempt > strategy.size()) {
            CompletableFuture<ApiResponse> future = new CompletableFuture<>();
            // TODO: Add TooManyAttemptsException
            future.completeExceptionally(new StyraRunException("Too many attempts"));
            return future;
        }

        return request.apply(gateway)
                .thenCompose((response) -> {
                    if (STATUS_CODES_TO_RETRY.contains(response.getStatusCode())) {
                        return retry(request, strategy, strategy.nextIfMatch(gateway), attempt + 1);
                    }
                    return CompletableFuture.completedFuture(response);
                })
                .exceptionallyCompose((e) -> {
                    if (e instanceof RetryException) {
                        return retry(request, strategy, strategy.nextIfMatch(gateway), attempt + 1);
                    }
                    throw new CompletionException(e);
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
                    logger.debug("Gateways: {}", gateways.stream()
                            .map((g) -> g.getUri().toString())
                            .collect(Collectors.joining(", ")));
                    discoveryStrategy = gatewaySelectionStrategyFactory.create(gateways);
                }
            }
        }
        return CompletableFuture.completedFuture(discoveryStrategy);
    }
}
