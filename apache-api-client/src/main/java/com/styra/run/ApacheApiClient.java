package com.styra.run;

import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder;
import org.apache.hc.client5.http.async.methods.SimpleRequestProducer;
import org.apache.hc.client5.http.async.methods.SimpleResponseConsumer;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.function.Callback;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.reactor.ssl.TlsDetails;
import org.apache.hc.core5.util.Timeout;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ApacheApiClient implements ApiClient {
    private static final Callback<Exception> exCallback = (e) -> System.out.println(e.getMessage());
    private final Config config;
    private final CloseableHttpAsyncClient client;

    public ApacheApiClient(Config config) {
        this(config,
                HttpAsyncClients.custom()
                        .setDefaultRequestConfig(RequestConfig.custom()
                                .setResponseTimeout(Timeout.ofMilliseconds(config.getRequestTimeout().toMillis()))
                                .setConnectionRequestTimeout(Timeout.ofMilliseconds(config.getConnectionTimeout().toMillis()))
                                .build())
                        .setIoReactorExceptionCallback(exCallback)
                        .disableAutomaticRetries()
                        .setIOReactorConfig(IOReactorConfig.custom()
                                .setSoTimeout(Timeout.ofMilliseconds(config.getConnectionTimeout().toMillis()))
                                .build())
                        .setConnectionManager(PoolingAsyncClientConnectionManagerBuilder.create()
                                .setTlsStrategy(ClientTlsStrategyBuilder.create()
                                        .setSslContext(config.getSslContext())
                                        .setTlsDetailsFactory(sslEngine ->
                                                new TlsDetails(sslEngine.getSession(), sslEngine.getApplicationProtocol()))
                                        .build())
                                .build())
                        .build());
    }

    public ApacheApiClient(Config config, CloseableHttpAsyncClient client) {
        this.config = config;
        this.client = client;
    }

    @Override
    public CompletableFuture<ApiResponse> request(Method method, URI uri, Map<String, String> headers, String body) {
        client.start();

        SimpleRequestBuilder requestBuilder = SimpleRequestBuilder.create(method.name())
                .setHeader("User-Agent", config.getUserAgent())
                .setUri(uri);

        headers.forEach(requestBuilder::setHeader);

        if (body != null && methodSupportsBody(method)) {
            requestBuilder.setBody(body, ContentType.APPLICATION_JSON);
        }

        SimpleHttpRequest request = requestBuilder.build();
        return FutureConverter.toCompletableFuture(client.execute(
                        SimpleRequestProducer.create(request),
                        SimpleResponseConsumer.create(),
                        new NoopCallback<>()))
                .thenApply(response -> new ApiResponse(response.getCode(), response.getBodyText()));
    }

    @Override
    public void close() {
        client.close(CloseMode.GRACEFUL);
    }

    private static boolean methodSupportsBody(Method method) {
        return method == Method.PUT || method == Method.POST;
    }
}

class NoopCallback<T> implements FutureCallback<T> {
    @Override
    public void completed(T result) {
        System.out.println("completed");
    }

    @Override
    public void failed(Exception ex) {
        System.out.println("failed");
    }

    @Override
    public void cancelled() {
        System.out.println("cancelled");
    }
}

class FutureConverter {
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    public static final Duration COMPLETION_POLLING_INTERVAL = Duration.ofMillis(3);

    static <T> CompletableFuture<T> toCompletableFuture(Future<T> future) {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();
        resolveOrSchedule(future, completableFuture);
        return completableFuture;
    }

    private static <T> void resolveOrSchedule(Future<T> producer, CompletableFuture<T> consumer) {
        if (producer.isDone()) {
            try {
                consumer.complete(producer.get());
            } catch (InterruptedException e) {
                consumer.completeExceptionally(e);
            } catch (ExecutionException e) {
                consumer.completeExceptionally(e.getCause());
            }
        } else if (producer.isCancelled()) {
            consumer.cancel(true);
        } else {
            scheduler.schedule(() -> resolveOrSchedule(producer, consumer),
                    COMPLETION_POLLING_INTERVAL.toMillis(), TimeUnit.MILLISECONDS);
        }
    }
}
