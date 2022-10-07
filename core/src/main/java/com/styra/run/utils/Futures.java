package com.styra.run.utils;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class Futures {
    public static <T> CompletableFuture<List<T>> allOf(List<CompletableFuture<T>> futures) {
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply((Void) -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
    }

    /**
     * Like calling {@link #async(Lambdas.CheckedFunction, Function)} with {@link Function#identity()}
     * as <code>exceptionMutator</code>.
     *
     * @see #async(Lambdas.CheckedFunction, Function)
     */
    public static <T, R> Function<T, R> async(Lambdas.CheckedFunction<T, R, Exception> unsafe) {
        return async(unsafe, Function.identity());
    }

    /**
     * Helper method that executes the provided <code>unsafe</code> function and wraps any thrown exception
     * in a {@link CompletionException}.
     * For use inside {@link CompletableFuture} callbacks to conveniently deal with checked exceptions.
     *
     * @param unsafe           the function to execute
     * @param exceptionMutator callback for wrapping or otherwise mutating any thrown exception
     * @return the return value of <code>unsafe</code>
     * @throws CompletionException wrapping any exception returned by <code>exceptionMutator</code>
     */
    public static <T, R> Function<T, R> async(Lambdas.CheckedFunction<T, R, Exception> unsafe,
                                              Function<Exception, Exception> exceptionMutator) {
        return v -> {
            try {
                return unsafe.apply(v);
            } catch (CompletionException e) {
                throw e; // Don't re-wrap any thrown CompletionException
            } catch (Exception e) {
                throw new CompletionException(exceptionMutator.apply(e));
            }
        };
    }

    /**
     * Like calling {@link #async(Lambdas.CheckedSupplier, Function)} with {@link Function#identity()}
     * as <code>exceptionMutator</code>.
     *
     * @see #async(Lambdas.CheckedSupplier, Function)
     */
    public static <T> Supplier<T> async(Lambdas.CheckedSupplier<T, Exception> unsafe) {
        return async(unsafe, Function.identity());
    }

    /**
     * Helper method that executes the provided <code>unsafe</code> supplier and wraps any thrown exception
     * in a {@link CompletionException}.
     * For use inside {@link CompletableFuture} callbacks to conveniently deal with checked exceptions.
     *
     * @param unsafe           the supplier to execute
     * @param exceptionMutator callback for wrapping or otherwise mutating any thrown exception
     * @return the return value of <code>unsafe</code>
     * @throws CompletionException wrapping any exception returned by <code>exceptionMutator</code>
     */
    public static <T> Supplier<T> async(Lambdas.CheckedSupplier<T, Exception> unsafe,
                                        Function<Exception, Exception> exceptionMutator) {
        return () -> {
            try {
                return unsafe.get();
            } catch (CompletionException e) {
                throw e; // Don't re-wrap any thrown CompletionException
            } catch (Exception e) {
                throw new CompletionException(exceptionMutator.apply(e));
            }
        };
    }

    /**
     * Helper method for starting an asynchronous flow.
     *
     * @param unsafe the supplier of the returned {@link CompletableFuture}:s value
     * @return a {@link CompletableFuture} with a value supplied by <code>unsafe</code>
     */
    public static <T> CompletableFuture<T> startAsync(Lambdas.CheckedSupplier<T, Exception> unsafe) {
        try {
            return CompletableFuture.completedFuture(unsafe.get());
        }  catch (Exception e) {
            return failedFuture(e);
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
