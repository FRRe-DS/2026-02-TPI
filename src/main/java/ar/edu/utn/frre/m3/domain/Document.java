package ar.edu.utn.frre.m3.domain;

import ar.edu.utn.frre.m3.domain.exception.DomainValidationException;
import ar.edu.utn.frre.m3.domain.exception.InvalidDocumentPeriodException;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

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
    private final Set<ServiceType> serviceTypes;
    private final int revision;
    private DocumentStatus status;
    private boolean isCurrent;
    private Long driverVersion;
    private String rejectionReason;
    private Instant reviewedAt;
    private String reviewedBy;
    private final Instant createdAt;

    public Document(UUID id, UUID driverId, UUID vehicleId, DocumentType documentType,
                    String documentNumber, String issuingEntity, Instant issuedAt,
                    Instant validUntil, String storageReference, int revision,
                    DocumentStatus status, boolean isCurrent, Long driverVersion,
                    String rejectionReason, Instant reviewedAt, String reviewedBy,
                    Instant createdAt) {
        this(id, driverId, vehicleId, documentType, documentNumber, issuingEntity, issuedAt,
                validUntil, storageReference, revision, status, isCurrent, driverVersion,
                rejectionReason, reviewedAt, reviewedBy, createdAt, Set.of());
    }

    public Document(UUID id, UUID driverId, UUID vehicleId, DocumentType documentType,
                    String documentNumber, String issuingEntity, Instant issuedAt,
                    Instant validUntil, String storageReference, int revision,
                    DocumentStatus status, boolean isCurrent, Long driverVersion,
                    String rejectionReason, Instant reviewedAt, String reviewedBy,
                    Instant createdAt, Set<ServiceType> serviceTypes) {
        this.id = Objects.requireNonNull(id, "document id cannot be null");
        this.driverId = Objects.requireNonNull(driverId, "driver id cannot be null");
        this.vehicleId = vehicleId;
        this.documentType = Objects.requireNonNull(documentType, "documentType cannot be null");
        this.documentNumber = Objects.requireNonNull(documentNumber, "documentNumber cannot be null");
        this.issuingEntity = issuingEntity;
        this.issuedAt = Objects.requireNonNull(issuedAt, "issuedAt cannot be null");
        this.validUntil = Objects.requireNonNull(validUntil, "validUntil cannot be null");
        this.storageReference = storageReference;
        this.serviceTypes = Set.copyOf(Objects.requireNonNull(serviceTypes, "serviceTypes cannot be null"));
        this.revision = Math.max(revision, 1);
        this.status = status != null ? status : DocumentStatus.PENDING;
        this.isCurrent = isCurrent;
        this.driverVersion = driverVersion != null && driverVersion > 0 ? driverVersion : 1L;
        this.rejectionReason = rejectionReason;
        this.reviewedAt = reviewedAt;
        this.reviewedBy = reviewedBy;
        this.createdAt = createdAt != null ? createdAt : Instant.now();

        if (!issuedAt.isBefore(validUntil)) {
            throw new InvalidDocumentPeriodException(issuedAt, validUntil);
        }
    }

    public static Document createPendingRevision(UUID driverId, UUID vehicleId,
                                                 DocumentType documentType, String documentNumber,
                                                 String issuingEntity, Instant issuedAt,
                                                 Instant validUntil, String storageReference,
                                                 int revision) {
        return new Document(
                UUID.randomUUID(), driverId, vehicleId, documentType, documentNumber,
                issuingEntity, issuedAt, validUntil, storageReference, revision,
                DocumentStatus.PENDING, true, 1L, null, null, null, Instant.now()
        );
    }

    public static Document createPendingLicense(UUID driverId, String documentNumber,
                                                Instant issuedAt, Instant validUntil,
                                                Set<ServiceType> serviceTypes,
                                                String storageReference, int revision,
                                                long driverVersion) {
        if (serviceTypes == null || serviceTypes.isEmpty()) {
            throw new DomainValidationException("License must allow at least one service type");
        }
        if (documentNumber == null || documentNumber.isBlank() || documentNumber.length() > 80) {
            throw new DomainValidationException("License number must be between 1 and 80 characters");
        }
        return new Document(UUID.randomUUID(), driverId, null, DocumentType.LICENSE,
                documentNumber.trim(), null, issuedAt, validUntil, storageReference, revision,
                DocumentStatus.PENDING, true, driverVersion, null, null, null, Instant.now(), serviceTypes);
    }

    public void review(ReviewDecision decision, String reason, String actor, long newDriverVersion) {
        String normalizedReason = validateReview(decision, reason);
        this.status = switch (decision) {
            case APPROVED -> DocumentStatus.APPROVED;
            case OBSERVED -> DocumentStatus.OBSERVED;
            case REJECTED -> DocumentStatus.REJECTED;
        };
        this.rejectionReason = normalizedReason;
        this.reviewedBy = actor != null && !actor.isBlank() ? actor : "OPERATOR";
        this.reviewedAt = Instant.now();
        this.driverVersion = newDriverVersion;
    }

    /** Validates a review before the enclosing use case changes the driver's version. */
    public String validateReview(ReviewDecision decision, String reason) {
        Objects.requireNonNull(decision, "Review decision must not be null");
        String normalizedReason = reason != null && !reason.isBlank() ? reason.trim() : null;
        if (normalizedReason != null && normalizedReason.length() > 500) {
            throw new DomainValidationException("Reason must not exceed 500 characters");
        }
        if (decision.requiresReason() && normalizedReason == null) {
            throw new DomainValidationException(decision.missingReasonMessage());
        }
        return normalizedReason;
    }

    public void markAsNonCurrent() { this.isCurrent = false; }
    public boolean isExpired(Instant currentInstant) { return !currentInstant.isBefore(this.validUntil); }

    public UUID getId() { return id; }
    public UUID getDriverId() { return driverId; }
    public UUID getVehicleId() { return vehicleId; }
    public DocumentType getDocumentType() { return documentType; }
    public String getDocumentNumber() { return documentNumber; }
    public String getIssuingEntity() { return issuingEntity; }
    public Instant getIssuedAt() { return issuedAt; }
    public Instant getValidUntil() { return validUntil; }
    public String getStorageReference() { return storageReference; }
    public Set<ServiceType> getServiceTypes() { return serviceTypes; }
    public int getRevision() { return revision; }
    public DocumentStatus getStatus() { return status; }
    public boolean isCurrent() { return isCurrent; }
    public void assignDriverVersion(long version) { if(version<1) throw new IllegalArgumentException("Invalid driver version"); this.driverVersion=version; }

    public Long getDriverVersion() { return driverVersion; }
    public String getRejectionReason() { return rejectionReason; }
    public Instant getReviewedAt() { return reviewedAt; }
    public String getReviewedBy() { return reviewedBy; }
    public Instant getCreatedAt() { return createdAt; }
}
