package com.styra.run.discovery;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SimpleGatewaySelectionStrategy implements GatewaySelectionStrategy {
    private final List<Gateway> gateways;
    private final AtomicInteger index = new AtomicInteger(0);

    private SimpleGatewaySelectionStrategy(List<Gateway> gateways) {
        this.gateways = gateways;
    }

    @Override
    public Gateway current() {
        return gateways.get(index.get());
    }

    @Override
    public Gateway next() {
        return gateways.get(index.updateAndGet((i) -> (i + 1) % gateways.size()));
    }

    @Override
    public int size() {
        return gateways.size();
    }

    public static class Factory implements GatewaySelectionStrategy.Factory {
        @Override
        public GatewaySelectionStrategy create(List<Gateway> gateways) {
            return new SimpleGatewaySelectionStrategy(gateways);
        }
    }
}
