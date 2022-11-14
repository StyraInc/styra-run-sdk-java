package com.styra.run;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.styra.run.utils.Futures.async;

/**
 * Sanitizes output by removing attributes and obfuscating errors.
 *
 * @param <S> the {@link Session session} type
 */
// TODO: By default, only pass-through boolean policy decisions; to protect against data siphoning.
// TODO: Configurable allow-list of rule paths.
public class Proxy<S extends Proxy.Session> {
    private static final Logger logger = LoggerFactory.getLogger(Proxy.class);

    private final StyraRun styraRun;
    private final InputTransformer<S> inputTransformer;

    public Proxy(StyraRun styraRun, InputTransformer<S> inputTransformer) {
        this.styraRun = styraRun;
        this.inputTransformer = inputTransformer;
    }

    public Proxy(StyraRun styraRun) {
        this.styraRun = styraRun;
        this.inputTransformer = (input, path, session) -> input;
    }

    // TODO: Use fine-grained (still obfuscated) exception types based on Styra Run back-end response
    public CompletableFuture<ListResult> proxy(BatchQuery batchQuery, S session) {
        Input<?> globalInput = inputTransformer.transform(batchQuery.getInput(), null, session);

        List<BatchQuery.Item> items = batchQuery.getItems().stream()
                .map((query) -> query.withInput(inputTransformer.transform(query.getInput(), query.getPath(), session)))
                .collect(Collectors.toList());

        return styraRun.batchQuery(items, globalInput)
                .exceptionally(async((e) -> {
                    logger.info("Batch query failed", e);
                    throw new StyraRunException("Batch query failed");
                }));
    }

    public interface Session {}

    @FunctionalInterface
    public interface InputTransformer<S extends Session> {
        Input<?> transform(Input<?> input, String path, S session);

        static <S extends Session> InputTransformer<S> identity() {
            return (input, path, session) -> input;
        }
    }
}
