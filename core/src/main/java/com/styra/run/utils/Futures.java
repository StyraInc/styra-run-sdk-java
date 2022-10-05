package com.styra.run.utils;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public final class Futures {
    public static <T> CompletableFuture<List<T>> allOf(List<CompletableFuture<T>> futures) {
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply((Void) -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
    }

    public static <T> CompletableFuture<T> async(Lambdas.CheckedSupplier<T, Exception> unsafe) {
        try {
            return CompletableFuture.completedFuture(unsafe.get());
        } catch (Exception e) {
            CompletableFuture<T> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    public static <T> CompletableFuture<T> failedFuture(Throwable e) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(e);
        return future;
    }

    public static Throwable unwrapException(Throwable e) {
        if (e instanceof CompletionException || e instanceof ExecutionException) {
            return unwrapException(e.getCause());
        }
        return e;
    }
}
