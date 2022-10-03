package com.styra.run.utils

import com.styra.run.ApiClient
import com.styra.run.ApiResponse

import java.util.concurrent.CompletableFuture

import static com.styra.run.utils.apiClients.exceptionalResult

class CountingApiClient implements ApiClient {
    def hitCount = 0
    Closure<CompletableFuture<ApiResponse>> resultSupplier = exceptionalResult(new Exception("Unknown Error"))

    @Override
    CompletableFuture<ApiResponse> request(Method method, URI uri, Map<String, String> headers, String body) {
        ++hitCount
        return resultSupplier()
    }
}

static Closure<CompletableFuture<ApiResponse>> exceptionalResult(Exception e) {
    return {-> CompletableFuture.failedFuture(e)} as Closure<CompletableFuture<ApiResponse>>
}
