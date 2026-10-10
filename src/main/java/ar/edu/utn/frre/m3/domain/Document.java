package ar.edu.utn.frre.m3.domain;

import ar.edu.utn.frre.m3.domain.exception.InvalidDocumentPeriodException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain entity representing a vehicle document (Insurance, Vehicle Registration).
 * Enforces business rules: validity periods, non-destructive versioning, and initial review status.
 */
public class Document {

    private final UUID id;
    private final UUID driverId;
    private final UUID vehicleId;
    private final DocumentType documentType;
    private final String documentNumber;
    private final String issuingEntity;
    private final Instant issuedAt;
    private final Instant validUntil;
    private final String storageReference;
    private final int revision;
    private final DocumentStatus status;
    private boolean isCurrent;
    private final Instant createdAt;

    private Document(
        UUID id,
        UUID driverId,
        UUID vehicleId,
        DocumentType documentType,
        String documentNumber,
        String issuingEntity,
        Instant issuedAt,
        Instant validUntil,
        String storageReference,
        int revision,
        DocumentStatus status,
        boolean isCurrent,
        Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id, "document id cannot be null");
        this.driverId = Objects.requireNonNull(driverId, "driver id cannot be null");
        this.vehicleId = Objects.requireNonNull(vehicleId, "vehicle id cannot be null");
        this.documentType = Objects.requireNonNull(documentType, "documentType cannot be null");
        this.documentNumber = Objects.requireNonNull(documentNumber, "documentNumber cannot be null");
        this.issuingEntity = issuingEntity;
        this.issuedAt = Objects.requireNonNull(issuedAt, "issuedAt cannot be null");
        this.validUntil = Objects.requireNonNull(validUntil, "validUntil cannot be null");
        this.storageReference = storageReference;
        this.revision = revision;
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.isCurrent = isCurrent;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");

        validatePeriod(issuedAt, validUntil);
    }

    /**
     * Factory method for creating a brand-new revision with initial PENDING status and current flag true.
     */
    public static Document createPendingRevision(
        UUID driverId,
        UUID vehicleId,
        DocumentType documentType,
        String documentNumber,
        String issuingEntity,
        Instant issuedAt,
        Instant validUntil,
        String storageReference,
        int revision
    ) {
        if (revision < 1) {
            throw new IllegalArgumentException("revision must be at least 1");
        }
        return new Document(
            UUID.randomUUID(),
            driverId,
            vehicleId,
            documentType,
            documentNumber,
            issuingEntity,
            issuedAt,
            validUntil,
            storageReference,
            revision,
            DocumentStatus.PENDING,
            true,
            Instant.now()
        );
    }

    /**
     * Reconstitutes an existing Document from persistence storage.
     */
    public static Document reconstitute(
        UUID id,
        UUID driverId,
        UUID vehicleId,
        DocumentType documentType,
        String documentNumber,
        String issuingEntity,
        Instant issuedAt,
        Instant validUntil,
        String storageReference,
        int revision,
        DocumentStatus status,
        boolean isCurrent,
        Instant createdAt
    ) {
        return new Document(
            id,
            driverId,
            vehicleId,
            documentType,
            documentNumber,
            issuingEntity,
            issuedAt,
            validUntil,
            storageReference,
            revision,
            status,
            isCurrent,
            createdAt
        );
    }

    /**
     * Validates that issuedAt is strictly before validUntil.
     */
    private static void validatePeriod(Instant issuedAt, Instant validUntil) {
        if (!issuedAt.isBefore(validUntil)) {
            throw new InvalidDocumentPeriodException(issuedAt, validUntil);
        }
    }

    /**
     * Marks this document revision as no longer current (e.g., superseded by a newer revision).
     */
    public void markAsNonCurrent() {
        this.isCurrent = false;
    }

    /**
     * Checks if this document has expired relative to a given instant (exclusive comparison).
     */
    public boolean isExpired(Instant currentInstant) {
        return !currentInstant.isBefore(this.validUntil);
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

    public DocumentType getDocumentType() {
        return documentType;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public String getIssuingEntity() {
        return issuingEntity;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getValidUntil() {
        return validUntil;
    }

    public String getStorageReference() {
        return storageReference;
    }

    public int getRevision() {
        return revision;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public boolean isCurrent() {
        return isCurrent;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Document document = (Document) o;
        return Objects.equals(id, document.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

