package com.styra.run.discovery;

import java.util.List;

public final class StaticGatewaySelector extends GatewaySelector {
    private final List<Gateway> gateways;

    public StaticGatewaySelector(GatewaySelectionStrategy.Factory gatewaySelectionStrategyFactory, int maxAttempts, List<Gateway> gateways) {
        super(gatewaySelectionStrategyFactory, maxAttempts);
        this.gateways = gateways;
    }

    @Override
    protected List<Gateway> fetchGateways() {
        return gateways;
    }
}
