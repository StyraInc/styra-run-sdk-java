package com.styra.run;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class LoggingApiClient implements ApiClient {
    private static final Logger logger = LoggerFactory.getLogger(ApiClient.class);
    private final ApiClient delegate;

    public LoggingApiClient(ApiClient delegate) {
        this.delegate = delegate;
    }

    @Override
    public CompletableFuture<ApiResponse> request(Method method, URI uri, Map<String, String> headers, String body) {
        UUID uuid = UUID.randomUUID();
        if (logger.isTraceEnabled()) {
            logger.trace("{} '{}'; uuid:{}; headers={}; body='{}'", method, uri, uuid, headers, body);
        } else {
            logger.debug("{} '{}'; uuid:{}", method, uri, uuid);
        }
        return delegate.request(method, uri, headers, body)
                .thenApply((response -> logResponse(response, uuid)));
    }

    private static ApiResponse logResponse(ApiResponse response, UUID uuid) {
        if (logger.isTraceEnabled()) {
            logger.trace("Response: uuid={}; status={}, body={}", uuid, response.getStatusCode(), response.getBody());
        } else {
            logger.debug("Response: uuid={}; status={}", uuid, response.getStatusCode());
        }
        return response;
    }
}
