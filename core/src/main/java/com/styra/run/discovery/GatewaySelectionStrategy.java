package com.styra.run.discovery;

import java.util.List;

public interface GatewaySelectionStrategy {
    Gateway current();
    Gateway next();
    int size();

    default Gateway nextIfMatch(Gateway gateway) {
        if (gateway.equals(current())) {
            synchronized (this) {
                if (gateway.equals(current())) {
                    return next();
                }
            }
        }
        return current();
    }

    @FunctionalInterface
    interface Factory {
        GatewaySelectionStrategy create(List<Gateway> gateways);
    }
}
