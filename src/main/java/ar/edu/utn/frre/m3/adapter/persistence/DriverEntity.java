package ar.edu.utn.frre.m3.adapter.persistence;

import ar.edu.utn.frre.m3.domain.Driver;
import ar.edu.utn.frre.m3.domain.QualificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad JPA mapeada a la tabla `drivers` de PostgreSQL.
 * Aislada del modelo de Dominio puro.
 */
@Entity
@Table(name = "drivers")
public class DriverEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "identity_subject", nullable = false, unique = true)
    private String identitySubject;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "qualification_status", nullable = false)
    private QualificationStatus qualificationStatus;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public DriverEntity() {
        // Constructor por defecto requerido por JPA
    }

    public DriverEntity(
            UUID id,
            String identitySubject,
            String name,
            QualificationStatus qualificationStatus,
            int version,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.identitySubject = identitySubject;
        this.name = name;
        this.qualificationStatus = qualificationStatus;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Mapeo desde la entidad de Dominio a la entidad JPA.
     */
    public static DriverEntity fromDomain(Driver driver) {
        return new DriverEntity(
                driver.getId(),
                driver.getIdentitySubject(),
                driver.getName(),
                driver.getQualificationStatus(),
                driver.getVersion(),
                driver.getCreatedAt(),
                driver.getUpdatedAt());
    }

    /**
     * Mapeo desde la entidad JPA al modelo puro de Dominio.
     */
    public Driver toDomain() {
        return new Driver(
                this.id,
                this.identitySubject,
                this.name,
                this.qualificationStatus,
                this.version,
                this.createdAt,
                this.updatedAt);
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public QualificationStatus getQualificationStatus() {
        return qualificationStatus;
    }

    public void setQualificationStatus(QualificationStatus qualificationStatus) {
        this.qualificationStatus = qualificationStatus;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof DriverEntity that))
            return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
