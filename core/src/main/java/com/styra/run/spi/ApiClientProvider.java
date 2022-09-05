package com.styra.run.spi;

import com.styra.run.ApiClient;

public interface ApiClientProvider {
    ApiClient create();
}
