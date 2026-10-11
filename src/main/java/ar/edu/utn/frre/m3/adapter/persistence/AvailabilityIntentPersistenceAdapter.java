package ar.edu.utn.frre.m3.adapter.persistence;

import ar.edu.utn.frre.m3.adapter.persistence.entity.AuditLogJpaEntity;
import ar.edu.utn.frre.m3.adapter.persistence.entity.AvailabilityIntentJpaEntity;
import ar.edu.utn.frre.m3.adapter.persistence.entity.IdempotencyRecordJpaEntity;
import ar.edu.utn.frre.m3.adapter.persistence.entity.OutboxJpaEntity;
import ar.edu.utn.frre.m3.adapter.persistence.repository.SpringDataAuditLogRepository;
import ar.edu.utn.frre.m3.adapter.persistence.repository.SpringDataAvailabilityIntentRepository;
import ar.edu.utn.frre.m3.adapter.persistence.repository.SpringDataIdempotencyRepository;
import ar.edu.utn.frre.m3.adapter.persistence.repository.SpringDataOutboxRepository;
import ar.edu.utn.frre.m3.application.port.AuditLogRepository;
import ar.edu.utn.frre.m3.application.port.AvailabilityIntentRepository;
import ar.edu.utn.frre.m3.application.port.IdempotencyRepository;
import ar.edu.utn.frre.m3.application.port.IdempotencyRepository.IdempotencyRecord;
import ar.edu.utn.frre.m3.application.port.OutboxRepository;
import ar.edu.utn.frre.m3.domain.AvailabilityIntent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Persistence adapter implementing all availability-related output ports:
 * {@link AvailabilityIntentRepository}, {@link AuditLogRepository},
 * {@link OutboxRepository} and {@link IdempotencyRepository}.
 *
 * <p>All writes happen inside the caller's transaction (UnitOfWork).
 */
@Component
public class AvailabilityIntentPersistenceAdapter
    implements AvailabilityIntentRepository, AuditLogRepository,
               OutboxRepository, IdempotencyRepository {

    /** Idempotency record retention: 7 days (architecture requirement). */
    private static final Duration IDEMPOTENCY_RETENTION = Duration.ofDays(7);

    private static final String M4_AVAILABILITY_INTENT_MESSAGE_TYPE = "m3.availability-intent.v1";

    private final SpringDataAvailabilityIntentRepository intentRepo;
    private final SpringDataAuditLogRepository auditRepo;
    private final SpringDataOutboxRepository outboxRepo;
    private final SpringDataIdempotencyRepository idempotencyRepo;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final String m4BaseUrl;

    public AvailabilityIntentPersistenceAdapter(
            SpringDataAvailabilityIntentRepository intentRepo,
            SpringDataAuditLogRepository auditRepo,
            SpringDataOutboxRepository outboxRepo,
            SpringDataIdempotencyRepository idempotencyRepo,
            ObjectMapper objectMapper,
            Clock clock,
            @org.springframework.beans.factory.annotation.Value("${m3.integration.m4-base-url:http://localhost:4004}") String m4BaseUrl) {
        this.intentRepo = Objects.requireNonNull(intentRepo);
        this.auditRepo = Objects.requireNonNull(auditRepo);
        this.outboxRepo = Objects.requireNonNull(outboxRepo);
        this.idempotencyRepo = Objects.requireNonNull(idempotencyRepo);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.clock = Objects.requireNonNull(clock);
        this.m4BaseUrl = Objects.requireNonNull(m4BaseUrl);
    }

    // ─── AvailabilityIntentRepository ────────────────────────────────────────

    @Override
    public Optional<AvailabilityIntent> findByDriverId(UUID driverId) {
        return intentRepo.findByDriverId(driverId).map(this::toDomain);
    }

    @Override
    public AvailabilityIntent save(AvailabilityIntent intent) {
        AvailabilityIntentJpaEntity entity = toJpa(intent);
        AvailabilityIntentJpaEntity saved = intentRepo.save(entity);
        return toDomain(saved);
    }

    // ─── AuditLogRepository ──────────────────────────────────────────────────

    @Override
    public void append(String entityType, UUID entityId, String operation,
                       String actor, String correlationId, String changes) {
        AuditLogJpaEntity entry = new AuditLogJpaEntity(
                UUID.randomUUID(), entityType, entityId,
                operation, actor, correlationId,
                Instant.now(clock), changes);
        auditRepo.save(entry);
    }

    // ─── OutboxRepository ────────────────────────────────────────────────────

    @Override
    public void enqueue(AvailabilityIntent intent, String correlationId) {
        String payload = buildM4Payload(intent, correlationId);
        String destination = m4BaseUrl +
            "/api/v1/drivers/" + intent.getDriverId() + "/availability-intent";

        OutboxJpaEntity entry = new OutboxJpaEntity(
                UUID.randomUUID(),
                destination,
                M4_AVAILABILITY_INTENT_MESSAGE_TYPE,
                "AvailabilityIntent",
                intent.getId(),
                intent.getDesiredVersion(),
                payload,
                "PENDING",
                0,
                Instant.now(clock),
                Instant.now(clock));
        outboxRepo.save(entry);
    }

    // ─── IdempotencyRepository ───────────────────────────────────────────────

    @Override
    public Optional<IdempotencyRecord> findByKey(
            String principal, String httpMethod, String path, String idempotencyKey) {
        return idempotencyRepo
            .findByPrincipalAndHttpMethodAndPathAndIdempotencyKey(
                    principal, httpMethod, path, idempotencyKey)
            .map(e -> new IdempotencyRecord(
                    e.getPrincipal(), e.getHttpMethod(), e.getPath(),
                    e.getIdempotencyKey(), e.getRequestHash(),
                    e.getResponseStatus(), e.getResponseBody()));
    }

    @Override
    public void save(IdempotencyRecord record) {
        Instant now = Instant.now(clock);
        IdempotencyRecordJpaEntity entity = new IdempotencyRecordJpaEntity(
                UUID.randomUUID(),
                record.principal(),
                record.httpMethod(),
                record.path(),
                record.idempotencyKey(),
                record.requestHash(),
                record.responseStatus(),
                record.responseBody(),
                now,
                now.plus(IDEMPOTENCY_RETENTION));
        idempotencyRepo.save(entity);
    }

    // ─── Mappers ─────────────────────────────────────────────────────────────

    private AvailabilityIntent toDomain(AvailabilityIntentJpaEntity e) {
        return new AvailabilityIntent(
                e.getId(), e.getDriverId(), e.getVehicleId(),
                e.isConnected(), e.isAvailable(),
                e.getStatus(), e.getDesiredVersion(), e.getAppliedVersion(),
                e.getCreatedAt(), e.getUpdatedAt(), e.getSyncedAt());
    }

    private AvailabilityIntentJpaEntity toJpa(AvailabilityIntent d) {
        return new AvailabilityIntentJpaEntity(
                d.getId(), d.getDriverId(), d.getVehicleId(),
                d.isConnected(), d.isAvailable(),
                d.getStatus(), d.getDesiredVersion(), d.getAppliedVersion(),
                d.getCreatedAt(), d.getUpdatedAt(), d.getSyncedAt());
    }

    private String buildM4Payload(AvailabilityIntent intent, String correlationId) {
        // Minimal payload matching INT-05 spec (M3 → M4)
        Map<String, Object> body = Map.of(
                "commandId", UUID.randomUUID().toString(),
                "driverId", intent.getDriverId().toString(),
                "vehicleId", intent.getVehicleId().toString(),
                "connected", intent.isConnected(),
                "available", intent.isAvailable(),
                "desiredVersion", intent.getDesiredVersion(),
                "correlationId", correlationId != null ? correlationId : "");
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize M4 payload", ex);
        }
    }
}
