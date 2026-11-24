package ar.edu.utn.frre.m3.adapter.observability;

import ar.edu.utn.frre.m3.application.usecase.RequestAvailabilityResult;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import java.util.List;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Adaptador externo a los casos de uso; envuelve también el interceptor transaccional. */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class DriverMetricsAspect {
    private final DoubleHistogram registration;
    private final DoubleHistogram operationalUpdate;

    public DriverMetricsAspect() {
        this(GlobalOpenTelemetry.getMeter("ar.edu.utn.frre.m3.business"));
    }

    DriverMetricsAspect(Meter meter) {
        registration = meter.histogramBuilder("m3.driver.registration.duration")
                .setExplicitBucketBoundariesAdvice(List.of(0.001, 0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0, 10.0))
                .setUnit("s").setDescription("Tiempo de alta hasta confirmación de persistencia").build();
        operationalUpdate = meter.histogramBuilder("m3.driver.operational_state.update.duration")
                .setExplicitBucketBoundariesAdvice(List.of(0.001, 0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0, 10.0))
                .setUnit("s").setDescription("Tiempo de registro local de conexión/disponibilidad; no incluye sincronización M4").build();
    }

    @Around("execution(public * ar.edu.utn.frre.m3.application.usecase.CreateDriverProfileUseCase.execute(..))")
    public Object registration(ProceedingJoinPoint invocation) throws Throwable {
        return measure(invocation, registration);
    }

    @Around("execution(public * ar.edu.utn.frre.m3.application.usecase.RequestAvailabilityService.execute(..))")
    public Object operationalUpdate(ProceedingJoinPoint invocation) throws Throwable {
        return measure(invocation, operationalUpdate);
    }

    private Object measure(ProceedingJoinPoint invocation, DoubleHistogram histogram) throws Throwable {
        long start = System.nanoTime();
        String entryPoint = "usecase";
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                && attributes.getRequest().getAttribute(RequestTimingFilter.START_NANOS) instanceof Long received) {
            start = received;
            entryPoint = "http";
        }
        String outcome = "error";
        boolean replay = false;
        try {
            Object result = invocation.proceed();
            replay = result instanceof RequestAvailabilityResult availability && availability.fromCache();
            outcome = "success";
            return result;
        } finally {
            if (!replay) {
                histogram.record((System.nanoTime() - start) / 1_000_000_000.0,
                        Attributes.builder().put("outcome", outcome).put("entry_point", entryPoint).build());
            }
        }
    }
}
