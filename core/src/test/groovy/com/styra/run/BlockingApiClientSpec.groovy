package com.styra.run

class BlockingApiClientSpec extends AbstractApiClientSpec {
    @Override
    ApiClient createApiClient(ApiClient.Config config) {
        return new BlockingApiClient(config)
    }

    @Override
    Class<BlockingApiClient> getApiClientType() {
        return BlockingApiClient
    }

    @Override
    Class<SocketTimeoutException> getExpectedConnectionTimeoutException() {
        return SocketTimeoutException
    }

    @Override
    Class<SocketTimeoutException> getExpectedRequestTimeoutException() {
        return SocketTimeoutException
    }

    @Override
    String getExpectedRequestTimeoutExceptionMessage() {
        return 'Read timed out'
    }
}
