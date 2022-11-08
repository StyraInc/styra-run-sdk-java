package com.styra.run

import java.net.http.HttpTimeoutException

class DefaultApiClientSpec extends AbstractApiClientSpec {
    @Override
    ApiClient createApiClient(ApiClient.Config config) {
        return new DefaultApiClient(config)
    }

    @Override
    Class<DefaultApiClient> getApiClientType() {
        return DefaultApiClient
    }

    @Override
    Class<HttpTimeoutException> getExpectedConnectionTimeoutException() {
        return HttpTimeoutException
    }

    @Override
    Class<HttpTimeoutException> getExpectedRequestTimeoutException() {
        return HttpTimeoutException
    }

    @Override
    String getExpectedRequestTimeoutExceptionMessage() {
        return 'request timed out'
    }
}
