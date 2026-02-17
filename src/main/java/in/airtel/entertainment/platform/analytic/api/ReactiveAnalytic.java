package in.airtel.entertainment.platform.analytic.api;

import in.airtel.entertainment.platform.analytic.core.AnalyticContextKeys;
import in.airtel.entertainment.platform.analytic.core.AnalyticJsonLogger;
import in.airtel.entertainment.platform.analytic.core.EntityExtractor;
import in.airtel.entertainment.platform.analytic.core.TransactionData;
import in.airtel.entertainment.platform.analytic.core.TransactionStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;

import java.util.Map;
import java.util.function.Function;

/**
 * Main API for reactive analytic transaction tracking.
 *
 * <p><b>Context propagation:</b> In Reactor, {@code contextWrite} enriches context for
 * operators <em>upstream</em> of it. {@code trace()} places {@code contextWrite} at the
 * outermost (downstream) position so all inner operators see the enriched context.
 *
 * <p><b>Typical usage with AOP:</b>
 * <pre>{@code
 * @AnalyseTransaction(name = "recommendation")
 * public Mono<CollectionDTO> getRecommendation(String id) {
 *     return doWork()
 *         .transform(ReactiveAnalytic.update("collectionId", id));
 * }
 * }</pre>
 *
 * <p><b>Operator-based usage:</b>
 * <pre>{@code
 * return fetchContent()
 *     .transform(ReactiveAnalytic.update("source", "multisource"))
 *     .transform(ReactiveAnalytic.trace("fetchContent"));
 * }</pre>
 */
public final class ReactiveAnalytic {

    private static final Logger LOG = LoggerFactory.getLogger(ReactiveAnalytic.class);

    private ReactiveAnalytic() {
    }

    /**
     * Wraps a Mono with a named transaction. Pushes a new TransactionData onto the
     * context stack and flushes it as JSON on completion or error.
     *
     * <p>contextWrite is placed downstream so all upstream operators (including
     * update/doOnEach calls) can see the enriched context.
     */
    public static <T> Function<Mono<T>, Mono<T>> trace(String transactionName) {
        return mono -> mono
                .doOnEach(signal -> {
                    if (signal.isOnComplete() || signal.isOnError()) {
                        flushTransaction(signal, transactionName);
                    }
                })
                .contextWrite(ctx -> {
                    TransactionStack stack = ctx.getOrDefault(
                            AnalyticContextKeys.TRANSACTION_STACK_KEY, TransactionStack.empty());
                    TransactionData txData = new TransactionData(transactionName);
                    return ctx.put(AnalyticContextKeys.TRANSACTION_STACK_KEY, stack.push(txData));
                });
    }

    /**
     * Wraps a Flux with a named transaction.
     */
    public static <T> Function<Flux<T>, Flux<T>> traceFlux(String transactionName) {
        return flux -> flux
                .doOnEach(signal -> {
                    if (signal.isOnComplete() || signal.isOnError()) {
                        flushTransaction(signal, transactionName);
                    }
                })
                .contextWrite(ctx -> {
                    TransactionStack stack = ctx.getOrDefault(
                            AnalyticContextKeys.TRANSACTION_STACK_KEY, TransactionStack.empty());
                    TransactionData txData = new TransactionData(transactionName);
                    return ctx.put(AnalyticContextKeys.TRANSACTION_STACK_KEY, stack.push(txData));
                });
    }

    /**
     * Sets a key-value pair on the current transaction at subscription time.
     * Must be upstream of (chained before) the corresponding {@code trace()} call,
     * or inside an {@code @AnalyseTransaction}-annotated method.
     */
    public static <T> Function<Mono<T>, Mono<T>> update(String key, Object value) {
        return mono -> mono.contextWrite(ctx -> {
            try {
                TransactionStack stack = ctx.getOrDefault(
                        AnalyticContextKeys.TRANSACTION_STACK_KEY, null);
                if (stack != null && !stack.isEmpty()) {
                    stack.peek().put(key, value);
                }
            } catch (Exception e) {
                LOG.warn("Analytic update failed: {}", e.getMessage());
            }
            return ctx;
        });
    }

    /**
     * Extracts @Analysed fields from entity and adds them to the current transaction.
     */
    public static <T> Function<Mono<T>, Mono<T>> updateEntity(Object entity) {
        return mono -> mono.contextWrite(ctx -> {
            try {
                TransactionStack stack = ctx.getOrDefault(
                        AnalyticContextKeys.TRANSACTION_STACK_KEY, null);
                if (stack != null && !stack.isEmpty()) {
                    Map<String, Object> extracted = EntityExtractor.extract(entity);
                    stack.peek().putAll(extracted);
                }
            } catch (Exception e) {
                LOG.warn("Analytic entity update failed: {}", e.getMessage());
            }
            return ctx;
        });
    }

    /**
     * Updates the current transaction from within a doOnEach callback.
     * Use this for dynamic values that depend on signal data.
     *
     * <pre>{@code
     * .doOnEach(signal -> {
     *     if (signal.isOnNext())
     *         ReactiveAnalytic.updateFromSignal(signal, "count", signal.get().size());
     * })
     * }</pre>
     */
    public static void updateFromSignal(Signal<?> signal, String key, Object value) {
        try {
            if (signal.getContextView().isEmpty()) return;
            TransactionStack stack = signal.getContextView().getOrDefault(
                    AnalyticContextKeys.TRANSACTION_STACK_KEY, null);
            if (stack == null || stack.isEmpty()) return;
            stack.peek().put(key, value);
        } catch (Exception e) {
            LOG.warn("Analytic update failed: {}", e.getMessage());
        }
    }

    /**
     * Extracts @Analysed fields from entity within a doOnEach callback.
     */
    public static void updateEntityFromSignal(Signal<?> signal, Object entity) {
        try {
            if (signal.getContextView().isEmpty()) return;
            TransactionStack stack = signal.getContextView().getOrDefault(
                    AnalyticContextKeys.TRANSACTION_STACK_KEY, null);
            if (stack == null || stack.isEmpty()) return;
            Map<String, Object> extracted = EntityExtractor.extract(entity);
            stack.peek().putAll(extracted);
        } catch (Exception e) {
            LOG.warn("Analytic entity update failed: {}", e.getMessage());
        }
    }

    /**
     * Returns the current TransactionData from the given context view, or null.
     */
    public static TransactionData currentTransaction(reactor.util.context.ContextView ctx) {
        TransactionStack stack = ctx.getOrDefault(
                AnalyticContextKeys.TRANSACTION_STACK_KEY, null);
        if (stack == null || stack.isEmpty()) return null;
        return stack.peek();
    }

    private static void flushTransaction(Signal<?> signal, String transactionName) {
        try {
            if (signal.getContextView().isEmpty()) return;
            TransactionStack stack = signal.getContextView().getOrDefault(
                    AnalyticContextKeys.TRANSACTION_STACK_KEY, null);
            if (stack == null || stack.isEmpty()) return;

            TransactionData current = stack.peek();
            if (current == null || !current.getTransactionName().equals(transactionName)) {
                return;
            }

            Throwable error = signal.isOnError() ? signal.getThrowable() : null;
            Map<String, Object> endMap = current.toEndMap(error);

            // Promote data to parent if nested
            TransactionData parent = stack.getParent();
            if (parent != null) {
                parent.putAll(current.getData());
            }

            AnalyticJsonLogger.log(endMap);
        } catch (Exception e) {
            LOG.warn("Analytic flush failed: {}", e.getMessage());
        }
    }
}
