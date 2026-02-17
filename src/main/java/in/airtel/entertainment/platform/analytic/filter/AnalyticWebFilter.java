package in.airtel.entertainment.platform.analytic.filter;

import in.airtel.entertainment.platform.analytic.core.AnalyticContextKeys;
import in.airtel.entertainment.platform.analytic.core.AnalyticJsonLogger;
import in.airtel.entertainment.platform.analytic.core.TransactionData;
import in.airtel.entertainment.platform.analytic.core.TransactionStack;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

public class AnalyticWebFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod().name();
        String transactionName = method + " " + path;

        TransactionData rootTx = new TransactionData(transactionName);
        rootTx.put("httpMethod", method);
        rootTx.put("requestPath", path);

        String correlationId = request.getHeaders().getFirst("correlationid");
        if (correlationId != null) {
            rootTx.put("correlationid", correlationId);
        }

        return chain.filter(exchange)
                .contextWrite(ctx -> {
                    TransactionStack stack = ctx.getOrDefault(
                            AnalyticContextKeys.TRANSACTION_STACK_KEY, TransactionStack.empty());
                    return ctx.put(AnalyticContextKeys.TRANSACTION_STACK_KEY, stack.push(rootTx));
                })
                .doFinally(signalType -> {
                    rootTx.put("signalType", signalType.name());
                    AnalyticJsonLogger.log(rootTx.toEndMap(null));
                });
    }
}
