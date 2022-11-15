package com.styra.run.discovery;

import com.styra.run.ApiClient;
import com.styra.run.ApiError;
import com.styra.run.ApiResponse;
import com.styra.run.Json;
import com.styra.run.Result;
import com.styra.run.exceptions.StyraRunException;
import com.styra.run.exceptions.StyraRunHttpException;
import com.styra.run.utils.Futures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.styra.run.ApiClient.Method.GET;
import static com.styra.run.utils.Lambdas.CheckedValue.tryWrap;
import static com.styra.run.utils.Url.appendPath;

public final class ApiGatewaySelector extends GatewaySelector {
    private static final Logger logger = LoggerFactory.getLogger(ApiGatewaySelector.class);

    private final ApiClient apiClient;
    private final Json json;
    private final URI envUri;
    private final Supplier<Map<String, String>> headerSupplier;

    public ApiGatewaySelector(GatewaySelectionStrategy.Factory discoveryStrategyFactory,
                              int maxAttempts,
                              ApiClient apiClient,
                              Json json,
                              URI envUri,
                              Supplier<Map<String, String>> headerSupplier) {
        super(discoveryStrategyFactory, maxAttempts);
        this.apiClient = apiClient;
        this.json = json;
        this.envUri = envUri;
        this.headerSupplier = headerSupplier;
    }

    // TODO: Return Future?
    protected List<Gateway> fetchGateways() throws StyraRunException {
        ApiResponse response;
        try {
            response = apiClient.requestBuilder(GET)
                    .uri(appendPath(envUri, "gateways"))
                    .headers(headerSupplier.get())
                    .request()
                    .get();
        } catch (Exception e) {
            Throwable cause = Futures.unwrapException(e);
            if (cause instanceof StyraRunException) {
                throw (StyraRunException) cause;
            }
            throw new StyraRunException("Unexpected error", cause);
        }

        if (!response.isSuccessful()) {
            throw new StyraRunHttpException(response.getStatusCode(), response.getBody(),
                    ApiError.fromApiResponse(response, json));
        }

        Result<?> result = Result.fromResponseMap(json.toOptionalMap(response.getBody())
                .orElseThrow(() -> new StyraRunException("Invalid response JSON")));

        return result.getListOf(Map.class)
                .stream()
                .map(tryWrap(Gateway::fromResponseMap))
                .collect(Collectors.toList())
                .stream()
                .map(valueOrException -> {
                    try {
                        return valueOrException.get();
                    } catch (URISyntaxException e) {
                        logger.warn("Ignoring invalid gateway", e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
