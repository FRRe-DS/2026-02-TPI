package ar.edu.utn.frre.m3.adapter.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity mapping the {@code audit_log} table (append-only).
 */
@Entity
@Table(name = "audit_log")
public class AuditLogJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "entity_type", nullable = false, length = 80)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "operation", nullable = false, length = 80)
    private String operation;

    @Column(name = "actor", length = 200)
    private String actor;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "changes", columnDefinition = "TEXT")
    private String changes;

    protected AuditLogJpaEntity() {}

    public AuditLogJpaEntity(
            UUID id, String entityType, UUID entityId,
            String operation, String actor, String correlationId,
            Instant occurredAt, String changes) {
        this.id = id;
        this.entityType = entityType;
        this.entityId = entityId;
        this.operation = operation;
        this.actor = actor;
        this.correlationId = correlationId;
        this.occurredAt = occurredAt;
        this.changes = changes;
    }

    public UUID getId() { return id; }
    public String getEntityType() { return entityType; }
    public UUID getEntityId() { return entityId; }
    public String getOperation() { return operation; }
    public String getActor() { return actor; }
    public String getCorrelationId() { return correlationId; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getChanges() { return changes; }
}
