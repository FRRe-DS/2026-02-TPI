package ar.edu.utn.frre.m3.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documents")
public class DocumentJpaEntity {

    @Id
    private UUID id;

    @Column(name = "driver_id", nullable = false)
    private UUID driverId;

    @Column(name = "vehicle_id")
    private UUID vehicleId;

    @Column(name = "kind", nullable = false, length = 30)
    private String kind;

    @Column(name = "revision", nullable = false)
    private Integer revision;

    @Column(name = "review_status", nullable = false, length = 20)
    private String reviewStatus;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "valid_until", nullable = false)
    private Instant validUntil;

    @Column(name = "is_current", nullable = false)
    private Boolean isCurrent;

    @Column(name = "service_types", length = 50)
    private String serviceTypes;

    @Column(name = "driver_version", nullable = false)
    private Long driverVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public DocumentJpaEntity() {
    }

    public DocumentJpaEntity(UUID id, UUID driverId, UUID vehicleId, String kind,
                             Integer revision, String reviewStatus, Instant issuedAt,
                             Instant validUntil, Boolean isCurrent, String serviceTypes,
                             Long driverVersion, Instant createdAt) {
        this.id = id;
        this.driverId = driverId;
        this.vehicleId = vehicleId;
        this.kind = kind;
        this.revision = revision;
        this.reviewStatus = reviewStatus;
        this.issuedAt = issuedAt;
        this.validUntil = validUntil;
        this.isCurrent = isCurrent;
        this.serviceTypes = serviceTypes;
        this.driverVersion = driverVersion;
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

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public Integer getRevision() {
        return revision;
    }

    public void setRevision(Integer revision) {
        this.revision = revision;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
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

    public Boolean getIsCurrent() {
        return isCurrent;
    }

    public void setIsCurrent(Boolean isCurrent) {
        this.isCurrent = isCurrent;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
