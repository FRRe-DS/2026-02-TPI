package ar.edu.utn.frre.m3.domain;

import ar.edu.utn.frre.m3.domain.exception.VersionMismatchException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad pura de Dominio para Conductor (Driver).
 * No contiene dependencias de Spring, JPA ni ningún framework de infraestructura.
 */
public class Driver {

    private final UUID id;
    private final String identitySubject;
    private String name;
    private QualificationStatus qualificationStatus;
    private int version;
    private final Instant createdAt;
    private Instant updatedAt;

    /**
     * Constructor de dominio para registrar un nuevo conductor.
     * Inicia con estado PENDING, versión 1 y marcas de tiempo actuales.
     */
    public Driver(String identitySubject, String name) {
        this(
            UUID.randomUUID(),
            validateIdentitySubject(identitySubject),
            validateName(name),
            QualificationStatus.PENDING,
            1,
            Instant.now(),
            Instant.now()
        );
    }

    /**
     * Constructor completo para reconstrucción desde persistencia.
     */
    public Driver(
            UUID id,
            String identitySubject,
            String name,
            QualificationStatus qualificationStatus,
            int version,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "El id no puede ser nulo");
        this.identitySubject = validateIdentitySubject(identitySubject);
        this.name = validateName(name);
        this.qualificationStatus = Objects.requireNonNull(qualificationStatus, "El estado de calificación no puede ser nulo");
        this.version = version > 0 ? version : 1;
        this.createdAt = Objects.requireNonNull(createdAt, "La fecha de creación no puede ser nula");
        this.updatedAt = Objects.requireNonNull(updatedAt, "La fecha de actualización no puede ser nula");
    }

    public Driver(UUID id, String identitySubject, String name,
                  QualificationStatus qualificationStatus, long version,
                  Instant createdAt, Instant updatedAt) {
        this(id, identitySubject, name, qualificationStatus, Math.toIntExact(version), createdAt, updatedAt);
    }

    private static String validateIdentitySubject(String identitySubject) {
        if (identitySubject == null || identitySubject.trim().isEmpty()) {
            throw new IllegalArgumentException("identity_subject no puede ser nulo ni vacío");
        }
        return identitySubject.trim();
    }

    private static String validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre no puede ser nulo ni vacío");
        }
        return name.trim();
    }

    public UUID getId() {
        return id;
    }

    public String getIdentitySubject() {
        return identitySubject;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return name;
    }

    public QualificationStatus getQualificationStatus() {
        return qualificationStatus;
    }

    public int getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updateName(String newName) {
        this.name = validateName(newName);
        this.updatedAt = Instant.now();
    }

    public void changeQualificationStatus(QualificationStatus newStatus) {
        this.qualificationStatus = Objects.requireNonNull(newStatus, "El nuevo estado no puede ser nulo");
        this.updatedAt = Instant.now();
    }

    public void incrementVersion() {
        this.version++;
        this.updatedAt = Instant.now();
    }

    public void validateVersion(long expectedVersion) {
        if (version != expectedVersion) {
            throw new VersionMismatchException(version, expectedVersion);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Driver driver)) return false;
        return Objects.equals(id, driver.id) || Objects.equals(identitySubject, driver.identitySubject);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, identitySubject);
    }

    @Override
    public String toString() {
        return "Driver{" +
                "id=" + id +
                ", identitySubject='" + identitySubject + '\'' +
                ", name='" + name + '\'' +
                ", qualificationStatus=" + qualificationStatus +
                ", version=" + version +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
