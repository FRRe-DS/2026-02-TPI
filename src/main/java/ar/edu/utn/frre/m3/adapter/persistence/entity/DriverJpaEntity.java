package ar.edu.utn.frre.m3.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "drivers")
public class DriverJpaEntity {

    @Id
    private UUID id;

    @Column(name = "identity_subject", unique = true)
    private String identitySubject;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "qualification_status", nullable = false, length = 20)
    private String qualificationStatus;

    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public DriverJpaEntity() {
    }

    public DriverJpaEntity(UUID id, String identitySubject, String displayName,
                           String qualificationStatus, Long version,
                           Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.identitySubject = identitySubject;
        this.displayName = displayName;
        this.qualificationStatus = qualificationStatus;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getIdentitySubject() {
        return identitySubject;
    }

    public void setIdentitySubject(String identitySubject) {
        this.identitySubject = identitySubject;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getQualificationStatus() {
        return qualificationStatus;
    }

    public void setQualificationStatus(String qualificationStatus) {
        this.qualificationStatus = qualificationStatus;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
