package com.styra.run.spi;

import com.styra.run.ApiClient;

public interface ApiClientFactory {
    ApiClient create(ApiClient.Config config);
}
