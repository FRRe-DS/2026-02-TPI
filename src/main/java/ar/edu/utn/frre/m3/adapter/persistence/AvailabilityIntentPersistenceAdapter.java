package ar.edu.utn.frre.m3.adapter.persistence;

import ar.edu.utn.frre.m3.adapter.persistence.entity.AvailabilityIntentJpaEntity;
import ar.edu.utn.frre.m3.adapter.persistence.entity.OutboxJpaEntity;
import ar.edu.utn.frre.m3.adapter.persistence.repository.SpringDataAvailabilityIntentRepository;
import ar.edu.utn.frre.m3.adapter.persistence.repository.SpringDataOutboxRepository;
import ar.edu.utn.frre.m3.application.port.AvailabilityIntentRepository;
import ar.edu.utn.frre.m3.application.port.OutboxRepository;
import ar.edu.utn.frre.m3.domain.AvailabilityIntent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Persistence adapter implementing {@link AvailabilityIntentRepository} and {@link OutboxRepository}.
 *
 * <p>All writes happen inside the caller's transaction (UnitOfWork).
 */
@Component
public class AvailabilityIntentPersistenceAdapter
    implements AvailabilityIntentRepository, OutboxRepository {

    private static final String M4_AVAILABILITY_INTENT_MESSAGE_TYPE = "m3.availability-intent.v1";

    private final SpringDataAvailabilityIntentRepository intentRepo;
    private final SpringDataOutboxRepository outboxRepo;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final String m4BaseUrl;

    public AvailabilityIntentPersistenceAdapter(
            SpringDataAvailabilityIntentRepository intentRepo,
            SpringDataOutboxRepository outboxRepo,
            ObjectMapper objectMapper,
            Clock clock,
            @Value("${m3.integration.m4-base-url:http://localhost:4004}") String m4BaseUrl) {
        this.intentRepo = Objects.requireNonNull(intentRepo);
        this.outboxRepo = Objects.requireNonNull(outboxRepo);
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

    // ─── OutboxRepository ────────────────────────────────────────────────────

    @Override
    public void enqueue(AvailabilityIntent intent, String correlationId) {
        UUID commandId=UUID.randomUUID();
        String payload = buildM4Payload(intent, correlationId,commandId);
        String destination = m4BaseUrl +
            "/api/v1/drivers/" + intent.getDriverId() + "/availability-intent";

        OutboxJpaEntity entry = new OutboxJpaEntity(
                commandId,
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

    private String buildM4Payload(AvailabilityIntent intent, String correlationId,UUID commandId) {
        Map<String, Object> body = Map.of(
                "commandId", commandId.toString(),
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
