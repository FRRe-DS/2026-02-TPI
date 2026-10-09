package ar.edu.utn.frre.m3.adapter.persistence.entity;

import ar.edu.utn.frre.m3.domain.DocumentStatus;
import ar.edu.utn.frre.m3.domain.DocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private DocumentType documentType;

    @Column(name = "document_number", nullable = false, length = 80)
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
    @Column(name = "status", nullable = false, length = 50)
    private DocumentStatus status;

    @Column(name = "is_current", nullable = false)
    private boolean current;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

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

    public DocumentStatus getStatus() {
        return status;
    }

    public void setStatus(DocumentStatus status) {
        this.status = status;
    }

    public boolean isCurrent() {
        return current;
    }

    public void setCurrent(boolean current) {
        this.current = current;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

