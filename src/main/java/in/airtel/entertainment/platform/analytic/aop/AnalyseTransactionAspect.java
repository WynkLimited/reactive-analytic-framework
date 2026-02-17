package in.airtel.entertainment.platform.analytic.aop;

import in.airtel.entertainment.platform.analytic.annotation.AnalyseTransaction;
import in.airtel.entertainment.platform.analytic.api.ReactiveAnalytic;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Aspect
public class AnalyseTransactionAspect {

    private static final Logger LOG = LoggerFactory.getLogger(AnalyseTransactionAspect.class);

    @Around("@annotation(in.airtel.entertainment.platform.analytic.annotation.AnalyseTransaction)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        AnalyseTransaction annotation = signature.getMethod().getAnnotation(AnalyseTransaction.class);
        String transactionName = annotation.name();

        Object result = joinPoint.proceed();

        if (result instanceof Mono<?> mono) {
            return mono.transform(ReactiveAnalytic.trace(transactionName));
        }

        if (result instanceof Flux<?> flux) {
            return flux.transform(ReactiveAnalytic.traceFlux(transactionName));
        }

        LOG.warn("@AnalyseTransaction on method {} returning non-reactive type; skipping.",
                signature.getMethod().getName());
        return result;
    }
}
