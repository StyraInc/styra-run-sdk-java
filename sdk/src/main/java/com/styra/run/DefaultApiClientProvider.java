package com.styra.run;

import com.styra.run.spi.ApiClientProvider;

public class DefaultApiClientProvider implements ApiClientProvider {
    @Override
    public ApiClient create() {
        return new DefaultApiClient();
    }
}
