package com.styra.run;

import com.styra.run.ApiClient.Config;
import com.styra.run.spi.ApiClientFactory;

public class OkHttpApiClientFactory implements ApiClientFactory {
    @Override
    public ApiClient create(Config config) {
        return new OkHttpApiClient();
    }
}
