package in.airtel.entertainment.platform.analytic.api;

import in.airtel.entertainment.platform.analytic.core.AnalyticContextKeys;
import in.airtel.entertainment.platform.analytic.core.TransactionData;
import in.airtel.entertainment.platform.analytic.core.TransactionStack;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

class ReactiveAnalyticTest {

    @Test
    void traceShouldPushTransactionOntoContext() {
        // deferContextual is upstream of trace()'s contextWrite, so it sees the enriched context
        Mono<String> mono = Mono.deferContextual(ctx -> {
            TransactionStack stack = ctx.get(AnalyticContextKeys.TRANSACTION_STACK_KEY);
            assertEquals("testTrace", stack.peek().getTransactionName());
            return Mono.just("hello");
        }).transform(ReactiveAnalytic.trace("testTrace"));

        StepVerifier.create(mono)
                .expectNext("hello")
                .verifyComplete();
    }

    @Test
    void traceFluxShouldPushTransactionOntoContext() {
        Flux<String> flux = Flux.just("a", "b", "c")
                .transform(ReactiveAnalytic.traceFlux("fluxTrace"));

        StepVerifier.create(flux)
                .expectNext("a", "b", "c")
                .verifyComplete();
    }

    @Test
    void updateShouldMutateCurrentTransaction() {
        // update() uses contextWrite, which is upstream of trace()'s contextWrite.
        // So the mutation runs after trace's contextWrite pushes the stack.
        Mono<String> mono = Mono.deferContextual(ctx -> {
            TransactionStack stack = ctx.get(AnalyticContextKeys.TRANSACTION_STACK_KEY);
            assertEquals("myValue", stack.peek().get("myKey"));
            return Mono.just("data");
        })
                .transform(ReactiveAnalytic.update("myKey", "myValue"))
                .transform(ReactiveAnalytic.trace("updateTest"));

        StepVerifier.create(mono)
                .expectNext("data")
                .verifyComplete();
    }

    @Test
    void traceShouldHandleErrors() {
        Mono<String> mono = Mono.<String>error(new RuntimeException("boom"))
                .transform(ReactiveAnalytic.trace("errorTrace"));

        StepVerifier.create(mono)
                .expectErrorMessage("boom")
                .verify();
    }

    @Test
    void nestedTracesShouldStack() {
        Mono<String> inner = Mono.just("inner")
                .transform(ReactiveAnalytic.trace("child"));

        Mono<String> outer = inner
                .transform(ReactiveAnalytic.trace("parent"));

        StepVerifier.create(outer)
                .expectNext("inner")
                .verifyComplete();
    }

    @Test
    void updateFromSignalShouldBeNoOpWithoutContext() {
        // Should not throw even when no context
        Mono<String> mono = Mono.just("noContext")
                .doOnEach(signal -> {
                    if (signal.isOnNext()) {
                        ReactiveAnalytic.updateFromSignal(signal, "key", "value");
                    }
                });

        StepVerifier.create(mono)
                .expectNext("noContext")
                .verifyComplete();
    }

    @Test
    void currentTransactionShouldReturnNullWithoutStack() {
        assertNull(ReactiveAnalytic.currentTransaction(
                reactor.util.context.Context.empty()));
    }

    @Test
    void updateFromSignalShouldWorkInsideTracedPipeline() {
        // doOnEach is upstream of trace()'s contextWrite, so it sees the enriched context
        Mono<String> mono = Mono.just("value")
                .doOnEach(signal -> {
                    if (signal.isOnNext()) {
                        ReactiveAnalytic.updateFromSignal(signal, "dynamicKey", signal.get());
                    }
                })
                .transform(ReactiveAnalytic.trace("signalTest"));

        StepVerifier.create(mono)
                .expectNext("value")
                .verifyComplete();
    }

    @Test
    void aopPatternShouldWork() {
        // Simulates AOP: method returns a Mono with update(), then AOP wraps with trace()
        Mono<String> methodResult = Mono.just("result")
                .transform(ReactiveAnalytic.update("status", "ok"));

        // AOP wraps with trace (downstream)
        Mono<String> aopWrapped = methodResult
                .transform(ReactiveAnalytic.trace("aopMethod"));

        StepVerifier.create(aopWrapped)
                .expectNext("result")
                .verifyComplete();
    }
}
