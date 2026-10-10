package ar.edu.utn.frre.m3.adapter.persistence.entity;

import ar.edu.utn.frre.m3.domain.DocumentStatus;
import ar.edu.utn.frre.m3.domain.DocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity mapping the 'documents' table.
 */
@Entity
@Table(name = "documents")
public class DocumentJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "driver_id", nullable = false)
    private UUID driverId;

    @Column(name = "vehicle_id")
    private UUID vehicleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", length = 50)
    private DocumentType documentType;

    @Column(name = "document_number", length = 80)
    private String documentNumber;

    @Column(name = "issuing_entity", length = 100)
    private String issuingEntity;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "valid_until", nullable = false)
    private Instant validUntil;

    @Column(name = "storage_reference", length = 200)
    private String storageReference;

    @Column(name = "revision", nullable = false)
    private int revision;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private DocumentStatus status;

    @Column(name = "is_current", nullable = false)
    private boolean current;

    @Column(name = "service_types", length = 50)
    private String serviceTypes;

    @Column(name = "driver_version")
    private Long driverVersion;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Transient
    private String customKind;

    @Transient
    private String customReviewStatus;

    public DocumentJpaEntity() {
    }

    public DocumentJpaEntity(
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
        boolean current,
        Instant createdAt
    ) {
        this.id = id;
        this.driverId = driverId;
        this.vehicleId = vehicleId;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.issuingEntity = issuingEntity;
        this.issuedAt = issuedAt;
        this.validUntil = validUntil;
        this.storageReference = storageReference;
        this.revision = revision;
        this.status = status;
        this.current = current;
        this.createdAt = createdAt;
    }

    public DocumentJpaEntity(
        UUID id,
        UUID driverId,
        UUID vehicleId,
        String kind,
        Integer revision,
        String reviewStatus,
        Instant issuedAt,
        Instant validUntil,
        Boolean isCurrent,
        String serviceTypes,
        Long driverVersion,
        Instant createdAt
    ) {
        this.id = id;
        this.driverId = driverId;
        this.vehicleId = vehicleId;
        setKind(kind);
        this.revision = revision != null ? revision : 1;
        setReviewStatus(reviewStatus);
        this.issuedAt = issuedAt;
        this.validUntil = validUntil;
        this.current = isCurrent != null && isCurrent;
        this.serviceTypes = serviceTypes;
        this.driverVersion = driverVersion;
        this.createdAt = createdAt;
    }

    public DocumentJpaEntity(
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
        boolean current,
        String serviceTypes,
        Long driverVersion,
        String rejectionReason,
        Instant reviewedAt,
        String reviewedBy,
        Instant createdAt
    ) {
        this.id = id;
        this.driverId = driverId;
        this.vehicleId = vehicleId;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.issuingEntity = issuingEntity;
        this.issuedAt = issuedAt;
        this.validUntil = validUntil;
        this.storageReference = storageReference;
        this.revision = revision;
        this.status = status;
        this.current = current;
        this.serviceTypes = serviceTypes;
        this.driverVersion = driverVersion;
        this.rejectionReason = rejectionReason;
        this.reviewedAt = reviewedAt;
        this.reviewedBy = reviewedBy;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getDriverId() {
        return driverId;
    }

    public void setDriverId(UUID driverId) {
        this.driverId = driverId;
    }

    public UUID getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(UUID vehicleId) {
        this.vehicleId = vehicleId;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
        if (documentType != null) {
            this.customKind = documentType.name();
        }
    }

    public String getKind() {
        if (documentType != null) {
            return documentType.name();
        }
        return customKind;
    }

    public void setKind(String kind) {
        if (kind != null) {
            try {
                this.documentType = DocumentType.valueOf(kind);
            } catch (IllegalArgumentException e) {
                this.documentType = null;
            }
            this.customKind = kind;
        } else {
            this.customKind = null;
            this.documentType = null;
        }
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getIssuingEntity() {
        return issuingEntity;
    }

    public void setIssuingEntity(String issuingEntity) {
        this.issuingEntity = issuingEntity;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Instant getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(Instant validUntil) {
        this.validUntil = validUntil;
    }

    public String getStorageReference() {
        return storageReference;
    }

    public void setStorageReference(String storageReference) {
        this.storageReference = storageReference;
    }

    public int getRevision() {
        return revision;
    }

    public void setRevision(int revision) {
        this.revision = revision;
    }

    public void setRevision(Integer revision) {
        this.revision = revision != null ? revision : 1;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public void setStatus(DocumentStatus status) {
        this.status = status;
        if (status != null) {
            this.customReviewStatus = status.name();
        }
    }

    public String getReviewStatus() {
        if (status != null) {
            return status.name();
        }
        return customReviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        if (reviewStatus != null) {
            try {
                this.status = DocumentStatus.valueOf(reviewStatus);
            } catch (IllegalArgumentException e) {
                this.status = null;
            }
            this.customReviewStatus = reviewStatus;
        } else {
            this.customReviewStatus = null;
            this.status = null;
        }
    }

    public boolean isCurrent() {
        return current;
    }

    public Boolean getIsCurrent() {
        return current;
    }

    public void setCurrent(boolean current) {
        this.current = current;
    }

    public void setIsCurrent(Boolean isCurrent) {
        this.current = isCurrent != null && isCurrent;
    }

    public String getServiceTypes() {
        return serviceTypes;
    }

    public void setServiceTypes(String serviceTypes) {
        this.serviceTypes = serviceTypes;
    }

    public Long getDriverVersion() {
        return driverVersion;
    }

    public void setDriverVersion(Long driverVersion) {
        this.driverVersion = driverVersion;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
