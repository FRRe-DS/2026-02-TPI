package ar.edu.utn.frre.m3.domain.model;

import ar.edu.utn.frre.m3.domain.exception.VersionMismatchException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Driver {

    private final UUID id;
    private String identitySubject;
    private String displayName;
    private QualificationStatus qualificationStatus;
    private long version;
    private final Instant createdAt;
    private Instant updatedAt;

    public Driver(UUID id, String identitySubject, String displayName,
                  QualificationStatus qualificationStatus, long version,
                  Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "Driver ID must not be null");
        this.identitySubject = identitySubject;
        this.displayName = Objects.requireNonNull(displayName, "Display name must not be null");
        this.qualificationStatus = qualificationStatus != null ? qualificationStatus : QualificationStatus.PENDING;
        this.version = version > 0 ? version : 1;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public void incrementVersion() {
        this.version++;
        this.updatedAt = Instant.now();
    }

    public void validateVersion(long expectedVersion) {
        if (this.version != expectedVersion) {
            throw new VersionMismatchException(this.version, expectedVersion);
        }
    }

    public UUID getId() {
        return id;
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

    public QualificationStatus getQualificationStatus() {
        return qualificationStatus;
    }

    public void setQualificationStatus(QualificationStatus qualificationStatus) {
        this.qualificationStatus = qualificationStatus;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
