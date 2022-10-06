package com.styra.run.test

import com.styra.run.ApiClient
import com.styra.run.ApiResponse

import java.util.concurrent.CompletableFuture

import static com.styra.run.test.apiClients.exceptionalResult

class CountingApiClient implements ApiClient {
    def hitCount = 0
    Closure<CompletableFuture<ApiResponse>> responseSupplier = exceptionalResult(new Exception("Unknown Error"))

    @Override
    CompletableFuture<ApiResponse> request(Method method, URI uri, Map<String, String> headers, String body) {
        ++hitCount
        return responseSupplier(method, uri, headers, body)
    }
}

static Closure<CompletableFuture<ApiResponse>> exceptionalResult(Exception e) {
    return { method, uri, headers, body -> CompletableFuture.<ApiResponse> failedFuture(e) }
}

static Closure<CompletableFuture<ApiResponse>> httpResult(int status, String body) {
    return { method, uri, headers, _ -> CompletableFuture.completedFuture(new ApiResponse(status, body)) }
}
