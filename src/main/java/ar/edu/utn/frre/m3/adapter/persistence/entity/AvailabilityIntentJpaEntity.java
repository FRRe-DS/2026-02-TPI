package ar.edu.utn.frre.m3.adapter.persistence.entity;

import ar.edu.utn.frre.m3.domain.IntentStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity mapping the {@code availability_intents} table.
 * Domain logic lives in {@link ar.edu.utn.frre.m3.domain.AvailabilityIntent}.
 */
@Entity
@Table(
    name = "availability_intents",
    uniqueConstraints = @UniqueConstraint(
        name = "availability_intents_driver_id_uq",
        columnNames = "driver_id"
    )
)
public class AvailabilityIntentJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "driver_id", nullable = false, updatable = false)
    private UUID driverId;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Column(name = "connected", nullable = false)
    private boolean connected;

    @Column(name = "available", nullable = false)
    private boolean available;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private IntentStatus status;

    @Column(name = "desired_version", nullable = false)
    private long desiredVersion;

    @Column(name = "applied_version")
    private Long appliedVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "synced_at")
    private Instant syncedAt;

    // ─── Required by JPA ────────────────────────────────────────────────────
    protected AvailabilityIntentJpaEntity() {}

    // ─── Full constructor used by the mapper ─────────────────────────────────
    public AvailabilityIntentJpaEntity(
            UUID id, UUID driverId, UUID vehicleId,
            boolean connected, boolean available,
            IntentStatus status, long desiredVersion, Long appliedVersion,
            Instant createdAt, Instant updatedAt, Instant syncedAt) {
        this.id = id;
        this.driverId = driverId;
        this.vehicleId = vehicleId;
        this.connected = connected;
        this.available = available;
        this.status = status;
        this.desiredVersion = desiredVersion;
        this.appliedVersion = appliedVersion;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.syncedAt = syncedAt;
    }

    // ─── Getters ─────────────────────────────────────────────────────────────
    public UUID getId() { return id; }
    public UUID getDriverId() { return driverId; }
    public UUID getVehicleId() { return vehicleId; }
    public boolean isConnected() { return connected; }
    public boolean isAvailable() { return available; }
    public IntentStatus getStatus() { return status; }
    public long getDesiredVersion() { return desiredVersion; }
    public Long getAppliedVersion() { return appliedVersion; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getSyncedAt() { return syncedAt; }
}
