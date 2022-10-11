package com.styra.run;

import com.styra.run.spi.ApiClientFactory;

import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

final class ApiClientLoader {
    private static final ServiceLoader<ApiClientFactory> loader = ServiceLoader.load(ApiClientFactory.class);

    static ApiClientFactory loadDefaultClient() {
        loader.reload();
        return StreamSupport.stream(loader.spliterator(), false)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No API Client implementation found on classpath"));
    }
}
