package com.styra.run;

import com.styra.run.spi.ApiClientProvider;

import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

final class ApiClientLoader {
    private static final ServiceLoader<ApiClientProvider> loader = ServiceLoader.load(ApiClientProvider.class);

    static ApiClientProvider loadDefaultClient() {
        loader.reload();
        return StreamSupport.stream(loader.spliterator(), false)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No API Client implementation found on classpath"));
    }
}
