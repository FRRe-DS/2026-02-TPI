package ar.edu.utn.frre.m3.adapter.observability;

import ar.edu.utn.frre.m3.application.port.DriverRepository;
import ar.edu.utn.frre.m3.application.usecase.CreateDriverProfileUseCase;
import ar.edu.utn.frre.m3.application.usecase.RequestAvailabilityResult;
import ar.edu.utn.frre.m3.domain.Driver;
import ar.edu.utn.frre.m3.domain.IntentStatus;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DriverMetricsAspectTest {
    @TestConfiguration
    @EnableAspectJAutoProxy
    @EnableTransactionManagement
    static class Config {
        @Bean InMemoryMetricReader reader() { return InMemoryMetricReader.create(); }
        @Bean(destroyMethod = "close") SdkMeterProvider provider(InMemoryMetricReader reader) {
            return SdkMeterProvider.builder().registerMetricReader(reader).build();
        }
        @Bean DriverMetricsAspect aspect(SdkMeterProvider provider) {
            return new DriverMetricsAspect(provider.get("test"));
        }
        @Bean DriverRepository repository() {
            DriverRepository repository = mock(DriverRepository.class);
            when(repository.save(any(Driver.class))).thenAnswer(i -> i.getArgument(0));
            return repository;
        }
        @Bean CreateDriverProfileUseCase create(DriverRepository repository) {
            return new CreateDriverProfileUseCase(repository);
        }
        @Bean CommitManager transactionManager(InMemoryMetricReader reader) {
            return new CommitManager(reader);
        }
    }

    static class CommitManager extends AbstractPlatformTransactionManager {
        final InMemoryMetricReader reader;
        boolean committed;
        boolean fail;
        CommitManager(InMemoryMetricReader reader) { this.reader = reader; }
        protected Object doGetTransaction() { return new Object(); }
        protected void doBegin(Object transaction, TransactionDefinition definition) { }
        protected void doCommit(DefaultTransactionStatus status) {
            assertTrue(reader.collectAllMetrics().isEmpty(), "No registrar éxito antes del commit");
            if (fail) throw new IllegalStateException("Commit failed");
            committed = true;
        }
        protected void doRollback(DefaultTransactionStatus status) { }
    }

    @Test void measuresFromHttpReceptionAfterCommit() {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            var request = new MockHttpServletRequest();
            request.setAttribute(RequestTimingFilter.START_NANOS, System.nanoTime() - 1_000_000_000L);
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
            try {
                context.getBean(CreateDriverProfileUseCase.class).execute("subject-test", "Test");
                assertTrue(context.getBean(CommitManager.class).committed);
                var points = context.getBean(InMemoryMetricReader.class).collectAllMetrics()
                        .iterator().next().getHistogramData().getPoints();
                assertEquals(1, points.size());
                var point = points.iterator().next();
                assertEquals(1, point.getCount());
                assertTrue(point.getSum() >= 1.0, "Incluye tiempo anterior al caso de uso");
                assertEquals("success", point.getAttributes().get(AttributeKey.stringKey("outcome")));
                assertEquals("http", point.getAttributes().get(AttributeKey.stringKey("entry_point")));
            } finally { RequestContextHolder.resetRequestAttributes(); }
        }
    }

    @Test void failedCommitIsNeverRecordedAsSuccess() {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            context.getBean(CommitManager.class).fail = true;
            assertThrows(IllegalStateException.class,
                    () -> context.getBean(CreateDriverProfileUseCase.class).execute("subject-test", "Test"));
            var point = context.getBean(InMemoryMetricReader.class).collectAllMetrics()
                    .iterator().next().getHistogramData().getPoints().iterator().next();
            assertEquals("error", point.getAttributes().get(AttributeKey.stringKey("outcome")));
            assertEquals(1, point.getCount());
        }
    }

    @Test void idempotentReplayDoesNotCountAsAnotherStateChange() throws Throwable {
        try (var reader = InMemoryMetricReader.create();
             var provider = SdkMeterProvider.builder().registerMetricReader(reader).build()) {
            var aspect = new DriverMetricsAspect(provider.get("test"));
            var invocation = mock(ProceedingJoinPoint.class);
            var replay = new RequestAvailabilityResult(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    true, true, IntentStatus.PENDING, 1, Instant.now(), true);
            when(invocation.proceed()).thenReturn(replay);
            assertSame(replay, aspect.operationalUpdate(invocation));
            assertTrue(reader.collectAllMetrics().isEmpty());
        }
    }

    @Test void operationalChangeRecordsSuccessAndPreservesErrors() throws Throwable {
        try (var reader = InMemoryMetricReader.create();
             var provider = SdkMeterProvider.builder().registerMetricReader(reader).build()) {
            var aspect = new DriverMetricsAspect(provider.get("test"));
            var invocation = mock(ProceedingJoinPoint.class);
            var result = new RequestAvailabilityResult(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    false, false, IntentStatus.PENDING, 1, Instant.now(), false);
            var failure = new IllegalArgumentException("Invalid state");
            when(invocation.proceed()).thenReturn(result).thenThrow(failure);
            assertSame(result, aspect.operationalUpdate(invocation));
            assertSame(failure, assertThrows(IllegalArgumentException.class, () -> aspect.operationalUpdate(invocation)));
            var metric = reader.collectAllMetrics().iterator().next();
            assertEquals("m3.driver.operational_state.update.duration", metric.getName());
            assertEquals(2, metric.getHistogramData().getPoints().size());
            assertTrue(metric.getHistogramData().getPoints().stream().allMatch(p -> p.getCount() == 1));
        }
    }
}
