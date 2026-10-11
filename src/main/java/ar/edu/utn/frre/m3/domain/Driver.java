package ar.edu.utn.frre.m3.domain;

import ar.edu.utn.frre.m3.domain.exception.VersionMismatchException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad pura de Dominio para Conductor (Driver).
 * Representa el agregado raíz de conductor según la arquitectura hexagonal de M3.
 * Libre de dependencias de Spring, JPA o frameworks de infraestructura.
 */
public class Driver {

    private final UUID id;
    private final String identitySubject;
    private String displayName;
    private QualificationStatus qualificationStatus;
    private String qualificationReason;
    private PersonalInfo personalInfo;
    private int version;
    private final Instant createdAt;
    private Instant updatedAt;

    /**
     * Constructor de dominio para registrar un nuevo conductor (HU-01).
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
     * Constructor para creación inicial con información personal (HU-02).
     */
    public Driver(UUID id, String identitySubject, PersonalInfo personalInfo, Instant now) {
        this(
                id,
                identitySubject,
                personalInfo != null ? personalInfo.getFullName() : "Conductor",
                QualificationStatus.PENDING,
                1,
                now != null ? now : Instant.now(),
                now != null ? now : Instant.now()
        );
        this.personalInfo = personalInfo;
        this.qualificationReason = "Perfil registrado, pendiente de revisión administrativa";
    }

    /**
     * Constructor completo desde persistencia o seeds de prueba con PersonalInfo (HU-02).
     */
    public Driver(UUID id,
                  String identitySubject,
                  PersonalInfo personalInfo,
                  QualificationInfo qualification,
                  int version,
                  Instant createdAt,
                  Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id es requerido");
        this.identitySubject = validateIdentitySubject(identitySubject);
        this.personalInfo = personalInfo;
        this.displayName = personalInfo != null ? personalInfo.getFullName() : "Conductor";
        this.qualificationStatus = qualification != null && qualification.getStatus() != null
                ? QualificationStatus.valueOf(qualification.getStatus().name())
                : QualificationStatus.PENDING;
        this.qualificationReason = qualification != null ? qualification.getReason() : null;
        this.version = version > 0 ? version : 1;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt es requerido");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt es requerido");
    }

    /**
     * Constructor estándar para reconstrucción desde persistencia.
     */
    public Driver(
            UUID id,
            String identitySubject,
            String displayName,
            QualificationStatus qualificationStatus,
            long version,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "El id no puede ser nulo");
        this.identitySubject = validateIdentitySubject(identitySubject);
        this.displayName = validateName(displayName);
        this.qualificationStatus = qualificationStatus != null ? qualificationStatus : QualificationStatus.PENDING;
        this.version = version > 0 ? (int) version : 1;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
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

    public String getDisplayName() {
        if (displayName != null && !displayName.isBlank()) {
            return displayName;
        }
        if (personalInfo != null) {
            return personalInfo.getFullName();
        }
        return "";
    }

    public String getName() {
        return getDisplayName();
    }

    public QualificationStatus getQualificationStatus() {
        return qualificationStatus;
    }

    public DriverStatus getStatus() {
        return qualificationStatus != null
                ? DriverStatus.valueOf(qualificationStatus.name())
                : DriverStatus.PENDING;
    }

    public String getQualificationReason() {
        return qualificationReason;
    }

    public QualificationInfo getQualification() {
        return new QualificationInfo(getStatus(), qualificationReason);
    }

    public PersonalInfo getPersonalInfo() {
        if (personalInfo != null) {
            return personalInfo;
        }
        String full = getDisplayName();
        String first = full.contains(" ") ? full.substring(0, full.indexOf(' ')) : full;
        String last = full.contains(" ") ? full.substring(full.indexOf(' ') + 1) : "";
        if (first.isBlank()) first = "Conductor";
        if (last.isBlank()) last = "Registrado";
        String email = identitySubject != null && identitySubject.contains("@")
                ? identitySubject
                : "driver-" + (id != null ? id.toString().substring(0, 8) : "default") + "@clonuber.com";
        return new PersonalInfo(first, last, "00000000", null, null, null, email);
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

    public boolean isOperable() {
        return qualificationStatus == QualificationStatus.APPROVED;
    }

    public boolean isOwnedBy(String subject) {
        return this.identitySubject != null && this.identitySubject.equals(subject);
    }

    public void validateVersion(long expectedVersion) {
        if (this.version != expectedVersion) {
            throw new VersionMismatchException(this.version, expectedVersion);
        }
    }

    public void incrementVersion() {
        this.version++;
        this.updatedAt = Instant.now();
    }

    public void updateName(String newName) {
        this.displayName = validateName(newName);
        this.updatedAt = Instant.now();
    }

    public void updateDisplayName(String newDisplayName) {
        updateName(newDisplayName);
    }

    public void changeQualificationStatus(QualificationStatus newStatus) {
        this.qualificationStatus = Objects.requireNonNull(newStatus, "El nuevo estado no puede ser nulo");
        this.updatedAt = Instant.now();
    }

    public void updatePersonalInfo(PersonalInfo newInfo, Instant now) {
        this.personalInfo = Objects.requireNonNull(newInfo, "La nueva información personal no puede ser nula");
        this.displayName = newInfo.getFullName();
        this.updatedAt = Objects.requireNonNull(now, "updatedAt es requerido");
        this.version++;
    }

    public void updateQualification(QualificationInfo newQualification, Instant now) {
        Objects.requireNonNull(newQualification, "newQualification es requerido");
        this.qualificationStatus = QualificationStatus.valueOf(newQualification.getStatus().name());
        this.qualificationReason = newQualification.getReason();
        this.updatedAt = Objects.requireNonNull(now, "updatedAt es requerido");
        this.version++;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Driver driver)) return false;
        return Objects.equals(id, driver.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Driver{" +
                "id=" + id +
                ", identitySubject='" + identitySubject + '\'' +
                ", displayName='" + getDisplayName() + '\'' +
                ", qualificationStatus=" + qualificationStatus +
                ", version=" + version +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
