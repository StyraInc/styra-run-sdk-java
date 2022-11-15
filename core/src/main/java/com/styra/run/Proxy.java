package com.styra.run;

import com.styra.run.exceptions.StyraRunException;
import com.styra.run.session.InputTransformer;
import com.styra.run.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.styra.run.utils.Futures.async;

/**
 * A helper construct for building HTTP proxy functionality.
 * <p>
 * The proxy wraps the {@link StyraRun#batchQuery(List, Input)} method, but also puts in place additional output sanitization
 * where attributes are removed and errors are obfuscated, in order to not leak sensitive information to the requesting client.
 * <p>
 * By default, non-boolean policy decisions are rejected, and replaced with an error;
 * this behaviour can be toggled off.
 * <p>
 * By default, result attributes are dropped.
 *
 * @param <S> the {@link Session session} type
 */
// TODO: Configurable allow-list of rule paths.
public class Proxy<S extends Session> {
    private static final Logger logger = LoggerFactory.getLogger(Proxy.class);

    private final StyraRun styraRun;
    private final InputTransformer<S> inputTransformer;
    private final boolean allowNonBooleanResults;
    private final boolean replaceForbiddenResultWithError;
    private final boolean dropAttributes;

    private Proxy(StyraRun styraRun,
                  InputTransformer<S> inputTransformer,
                  boolean allowNonBooleanResults,
                  boolean replaceForbiddenResultWithError,
                  boolean dropAttributes) {
        this.styraRun = styraRun;
        this.inputTransformer = inputTransformer;
        this.allowNonBooleanResults = allowNonBooleanResults;
        this.replaceForbiddenResultWithError = replaceForbiddenResultWithError;
        this.dropAttributes = dropAttributes;
    }

    /**
     * Call {@link StyraRun#batchQuery(List, Input)} with output sanitization.
     *
     * @param batchQuery the query to execute
     * @param session    the {@link Session} for which to execute the query
     * @return a {@link CompletableFuture} carrying the {@link ListResult result} of the query
     */
    // TODO: Use fine-grained (still obfuscated) exception types based on Styra Run back-end response
    public CompletableFuture<ListResult> proxy(BatchQuery batchQuery, S session) {
        Input<?> globalInput = inputTransformer.transform(batchQuery.getInput(), null, session);

        List<BatchQuery.Item> items = batchQuery.getItems().stream()
                .map((query) -> query.withInput(inputTransformer.transform(query.getInput(), query.getPath(), session)))
                .collect(Collectors.toList());

        return styraRun.batchQuery(items, globalInput)
                .thenApply(this::sanitizeResults)
                .exceptionally(async((e) -> {
                    logger.info("Batch query failed", e);
                    throw new StyraRunException("Batch query failed");
                }));
    }

    private ListResult sanitizeResults(ListResult listResult) {
        List<Result<?>> entries = listResult.get().stream()
                .map(result -> {
                    if (!result.hasValue() || result.isBooleanValue() || allowNonBooleanResults) {
                        return dropAttributes ? result.withoutAttributes() : result;
                    } else if (replaceForbiddenResultWithError) {
                        return new ApiError("invalid_result", "Result was not boolean").toResult();
                    }
                    return Result.empty();
                })
                .collect(Collectors.toList());

        Map<String, ?> attributes;
        if (dropAttributes) {
            attributes = Collections.emptyMap();
        } else {
            attributes = listResult.getAttributes();
        }

        return new ListResult(entries, attributes);
    }

    public static <S extends Session> Builder<S> builder(StyraRun styraRun) {
        return new Builder<>(styraRun);
    }

    public static class Builder<S extends Session> {
        private final StyraRun styraRun;
        private InputTransformer<S> inputTransformer = InputTransformer.identity();
        private boolean allowNonBooleanResults = false;
        private boolean replaceForbiddenResultWithError = true;
        private boolean dropAttributes = true;

        public Builder(StyraRun styraRun) {
            this.styraRun = styraRun;
        }

        /**
         * Set the {@link InputTransformer} for modifying incoming input; e.g. to inject session information used by
         * the queried policy rules.
         * By default, {@link InputTransformer#identity()} is used.
         *
         * @param inputTransformer the {@link InputTransformer}
         * @return this builder
         */
        public Builder<S> inputTransformer(InputTransformer<S> inputTransformer) {
            this.inputTransformer = inputTransformer;
            return this;
        }

        /**
         * If set to <code>true</code>, non-boolean results that aren't empty will be replaced by either an empty result
         * or an error result, based on the value assigned to {@link #replaceForbiddenResultWithError(boolean)}.
         *
         * @param allow whether non-boolean results should be allowed or removed
         * @return this builder
         * @see #replaceForbiddenResultWithError(boolean)
         */
        public Builder<S> allowNonBooleanResults(boolean allow) {
            allowNonBooleanResults = allow;
            return this;
        }

        /**
         * If set to <code>true</code>, individual {@link Result result} entries in the result list returned by calls to
         * {@link Proxy#proxy(BatchQuery, Session)} that are rejected by output sanitization will we replaced with error results.
         * If set to <code>false</code>, rejected result entries are replaced with empty results.
         *
         * @param replace whether to replace forbidden results with an error result
         * @return this builder
         * @see #allowNonBooleanResults(boolean)
         */
        public Builder<S> replaceForbiddenResultWithError(boolean replace) {
            replaceForbiddenResultWithError = replace;
            return this;
        }

        /**
         * If set to <code>true</code>, attributes on individual {@link Result result} entries are dropped in the
         * result list returned by calls to {@link Proxy#proxy(BatchQuery, Session)}.
         *
         * @param drop whether to drop result attributes
         * @return this builder
         */
        public Builder<S> dropAttributes(boolean drop) {
            dropAttributes = drop;
            return this;
        }

        public Proxy<S> build() {
            return new Proxy<>(styraRun, inputTransformer, allowNonBooleanResults,
                    replaceForbiddenResultWithError, dropAttributes);
        }
    }
}
