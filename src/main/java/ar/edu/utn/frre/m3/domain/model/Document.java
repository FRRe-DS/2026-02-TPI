package ar.edu.utn.frre.m3.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Document {

    private final UUID id;
    private final UUID driverId;
    private final UUID vehicleId;
    private final DocumentKind kind;
    private final int revision;
    private ReviewStatus reviewStatus;
    private final Instant issuedAt;
    private final Instant validUntil;
    private final boolean isCurrent;
    private final List<ServiceType> serviceTypes;
    private long driverVersion;
    private final Instant createdAt;

    public Document(UUID id, UUID driverId, UUID vehicleId, DocumentKind kind,
                    int revision, ReviewStatus reviewStatus, Instant issuedAt,
                    Instant validUntil, boolean isCurrent, List<ServiceType> serviceTypes,
                    long driverVersion, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "Document ID must not be null");
        this.driverId = Objects.requireNonNull(driverId, "Driver ID must not be null");
        this.vehicleId = vehicleId;
        this.kind = Objects.requireNonNull(kind, "Document kind must not be null");
        this.revision = Math.max(revision, 1);
        this.reviewStatus = reviewStatus != null ? reviewStatus : ReviewStatus.PENDING;
        this.issuedAt = Objects.requireNonNull(issuedAt, "IssuedAt must not be null");
        this.validUntil = Objects.requireNonNull(validUntil, "ValidUntil must not be null");
        this.isCurrent = isCurrent;
        this.serviceTypes = serviceTypes != null ? new ArrayList<>(serviceTypes) : Collections.emptyList();
        this.driverVersion = driverVersion > 0 ? driverVersion : 1;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public void review(ReviewDecision decision, long newDriverVersion) {
        Objects.requireNonNull(decision, "Review decision must not be null");
        switch (decision) {
            case APPROVED -> this.reviewStatus = ReviewStatus.APPROVED;
            case OBSERVED -> this.reviewStatus = ReviewStatus.OBSERVED;
            case REJECTED -> this.reviewStatus = ReviewStatus.REJECTED;
        }
        this.driverVersion = newDriverVersion;
    }

    public UUID getId() {
        return id;
    }

    public UUID getDriverId() {
        return driverId;
    }

    public UUID getVehicleId() {
        return vehicleId;
    }

    public DocumentKind getKind() {
        return kind;
    }

    public int getRevision() {
        return revision;
    }

    public ReviewStatus getReviewStatus() {
        return reviewStatus;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getValidUntil() {
        return validUntil;
    }

    public boolean isCurrent() {
        return isCurrent;
    }

    public List<ServiceType> getServiceTypes() {
        return Collections.unmodifiableList(serviceTypes);
    }

    public long getDriverVersion() {
        return driverVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
