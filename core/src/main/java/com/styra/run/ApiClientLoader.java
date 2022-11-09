package com.styra.run;

import com.styra.run.spi.ApiClientFactory;

import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

final class ApiClientLoader {
    private static final ServiceLoader<ApiClientFactory> loader = ServiceLoader.load(ApiClientFactory.class);

    static ApiClient load(ApiClient.Config config) {
        loader.reload();
        return StreamSupport.stream(loader.spliterator(), false)
                .findFirst()
                .map((factory) -> factory.create(config))
                .orElse(new BlockingApiClient(config));
    }
}
