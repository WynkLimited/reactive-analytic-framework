package in.airtel.entertainment.platform.analytic.integration;

import in.airtel.entertainment.platform.analytic.annotation.AnalyseTransaction;
import in.airtel.entertainment.platform.analytic.aop.AnalyseTransactionAspect;
import in.airtel.entertainment.platform.analytic.api.ReactiveAnalytic;
import in.airtel.entertainment.platform.analytic.autoconfigure.ReactiveAnalyticAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {
        ReactiveAnalyticAutoConfiguration.class,
        AopAutoConfiguration.class,
        AutoConfigurationIntegrationTest.TestConfig.class
})
class AutoConfigurationIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private SampleService sampleService;

    @Test
    void shouldRegisterAspectBean() {
        assertTrue(context.containsBean("analyseTransactionAspect"));
        assertNotNull(context.getBean(AnalyseTransactionAspect.class));
    }

    @Test
    void shouldNotRegisterWebFilterWithoutProperty() {
        assertFalse(context.containsBean("analyticWebFilter"));
    }

    @Test
    void aopShouldWrapAnnotatedMethod() {
        Mono<String> result = sampleService.doWork();

        StepVerifier.create(result)
                .expectNext("done")
                .verifyComplete();
    }

    @Configuration
    @EnableAspectJAutoProxy
    static class TestConfig {
        @Bean
        SampleService sampleService() {
            return new SampleService();
        }
    }

    static class SampleService {
        @AnalyseTransaction(name = "sampleWork")
        public Mono<String> doWork() {
            return Mono.just("done")
                    .transform(ReactiveAnalytic.update("status", "completed"));
        }
    }
}
