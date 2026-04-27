package in.airtel.entertainment.platform.analytic.filter;

import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Compatibility filter for applications that still set
 * {@code reactive.analytic.webfilter.enabled=true}.
 *
 * <p>HTTP root transaction logging was intentionally removed because raw request
 * paths create high-cardinality transaction names. Analytic logging should start
 * at {@code @AnalyseTransaction} or explicit {@code ReactiveAnalytic.trace(...)}
 * boundaries.
 */
public class AnalyticWebFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return chain.filter(exchange);
    }
}
