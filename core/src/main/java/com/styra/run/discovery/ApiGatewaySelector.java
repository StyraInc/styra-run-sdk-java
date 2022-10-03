package com.styra.run.discovery;

import com.styra.run.ApiClient;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

public final class ApiGatewaySelector extends GatewaySelector {
    private final ApiClient apiClient;
    private final URI envUri;

    public ApiGatewaySelector(GatewaySelectionStrategy.Factory discoveryStrategyFactory, int maxAttempts, ApiClient apiClient, URI envUri) {
        super(discoveryStrategyFactory, maxAttempts);
        this.apiClient = apiClient;
        this.envUri = envUri;
    }

    protected List<Gateway> fetchGateways() {
        return Arrays.asList(
                new Gateway(URI.create("https://localhost:1111")),
                new Gateway(URI.create("https://localhost:2222"))
        );
    }
}
