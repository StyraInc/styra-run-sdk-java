package com.styra.run;

import com.styra.run.spi.ApiClientProvider;

public class OkHttpApiClientProvider implements ApiClientProvider {
    @Override
    public ApiClient create() {
        return new OkHttpApiClient();
    }
}
