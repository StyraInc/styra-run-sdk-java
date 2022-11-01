package com.styra.run;

import com.styra.run.spi.ApiClientFactory;

public class ApacheApiClientFactory implements ApiClientFactory {
    @Override
    public ApiClient create(ApiClient.Config config) {
        return new ApacheApiClient(config);
    }
}
