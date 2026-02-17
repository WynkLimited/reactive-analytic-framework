package in.airtel.entertainment.platform.analytic.autoconfigure;

import in.airtel.entertainment.platform.analytic.aop.AnalyseTransactionAspect;
import in.airtel.entertainment.platform.analytic.filter.AnalyticWebFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Mono;

@AutoConfiguration
@ConditionalOnClass(Mono.class)
public class ReactiveAnalyticAutoConfiguration {

    @Bean
    public AnalyseTransactionAspect analyseTransactionAspect() {
        return new AnalyseTransactionAspect();
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    @ConditionalOnProperty(name = "reactive.analytic.webfilter.enabled", havingValue = "true")
    public AnalyticWebFilter analyticWebFilter() {
        return new AnalyticWebFilter();
    }
}
