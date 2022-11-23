package com.styra.run.test

import com.fasterxml.jackson.jr.ob.JSON
import com.styra.run.ApiClient
import com.styra.run.ApiResponse
import com.styra.run.Result

import java.util.concurrent.CompletableFuture
import java.util.function.BiFunction

import static com.styra.run.test.apiClients.exceptionalResult

class CountingApiClient implements ApiClient {
    def hitCount = 0
    Closure<CompletableFuture<ApiResponse>> responseSupplier = exceptionalResult(new Exception("Unknown Error"))

    @Override
    CompletableFuture<ApiResponse> request(Method method, URI uri, Map<String, String> headers, String body) {
        ++hitCount
        return responseSupplier(method, uri, headers, body)
    }

    @Override
    void close() {
    }
}

class MockApiClient implements ApiClient {
    def index = 0
    List<Closure<CompletableFuture<ApiResponse>>> responseSuppliers

    @Override
    CompletableFuture<ApiResponse> request(Method method, URI uri, Map<String, String> headers, String body) {
        assert index < responseSuppliers.size(): 'unexpected request'

        return responseSuppliers[index++](method, uri, headers, body)
    }

    void assertExhausted() {
        assert index == responseSuppliers.size(): 'Not all response suppliers on ApiClient called'
    }

    @Override
    void close() throws Exception {
    }
}

static MockApiClient mockApiClient(List<Closure<CompletableFuture<ApiResponse>>> responseSuppliers) {
    return new MockApiClient(responseSuppliers: responseSuppliers)
}

static Closure<CompletableFuture<ApiResponse>> reply(ApiClient.Method expectedMethod, String expectedUri,
                                                     BiFunction<Map<String, String>, String, ApiResponse> handle) {
    return { method, uri, headers, body ->
        assert method == expectedMethod
        assert uri.toString() == expectedUri
        return CompletableFuture.completedFuture(handle(headers, body)) } as Closure<CompletableFuture<ApiResponse>>
}

static Closure<CompletableFuture<ApiResponse>> exceptionalResult(Exception e) {
    return { method, uri, headers, body -> CompletableFuture.<ApiResponse> failedFuture(e) }
}

static Closure<CompletableFuture<ApiResponse>> httpResult(int status, String body) {
    return { method, uri, headers, _ -> CompletableFuture.completedFuture(new ApiResponse(status, body)) }
}

static ApiResponse response(int status, value) {
    return response(status, new Result<Object>(value))
}

static ApiResponse response(int status, Result<?> result) {
    return new ApiResponse(status, JSON.std.asString(result.toMap()))
}
