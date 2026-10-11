package ar.edu.utn.frre.m3.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad de Dominio Agregada: Conductor (Driver).
 * Representa al conductor dentro de M3, su identidad vinculada a M1 y su estado de habilitación.
 * Diseñada como clase pura de Java sin dependencias de frameworks (Spring, JPA).
 */
public class Driver {
    private final UUID id;
    private final String identitySubject;
    private PersonalInfo personalInfo;
    private QualificationInfo qualification;
    private int version;
    private final Instant createdAt;
    private Instant updatedAt;

    /**
     * Constructor para creación de un nuevo conductor (registro inicial).
     */
    public Driver(UUID id, String identitySubject, PersonalInfo personalInfo, Instant now) {
        if (id == null) {
            throw new IllegalArgumentException("El ID del conductor no puede ser nulo");
        }
        if (identitySubject == null || identitySubject.isBlank()) {
            throw new IllegalArgumentException("El identitySubject (M1) no puede ser nulo ni estar vacío");
        }
        if (personalInfo == null) {
            throw new IllegalArgumentException("La información personal no puede ser nula");
        }
        if (now == null) {
            throw new IllegalArgumentException("La fecha de creación no puede ser nula");
        }

        this.id = id;
        this.identitySubject = identitySubject.trim();
        this.personalInfo = personalInfo;
        this.qualification = QualificationInfo.pending();
        this.version = 1;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Constructor completo para reconstrucción desde persistencia.
     */
    public Driver(UUID id,
                  String identitySubject,
                  PersonalInfo personalInfo,
                  QualificationInfo qualification,
                  int version,
                  Instant createdAt,
                  Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id es requerido");
        this.identitySubject = Objects.requireNonNull(identitySubject, "identitySubject es requerido");
        this.personalInfo = Objects.requireNonNull(personalInfo, "personalInfo es requerido");
        this.qualification = Objects.requireNonNull(qualification, "qualification es requerido");
        this.version = version;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt es requerido");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt es requerido");
    }

    public UUID getId() {
        return id;
    }

    public String getIdentitySubject() {
        return identitySubject;
    }

    public PersonalInfo getPersonalInfo() {
        return personalInfo;
    }

    public QualificationInfo getQualification() {
        return qualification;
    }

    public DriverStatus getStatus() {
        return qualification.getStatus();
    }

    public String getQualificationReason() {
        return qualification.getReason();
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

    /**
     * Comprueba si el conductor es apto para operar.
     */
    public boolean isOperable() {
        return qualification.isOperable();
    }

    /**
     * Verifica si una identidad dada (subject de M1) coincide con el propietario del perfil.
     */
    public boolean isOwnedBy(String subject) {
        return this.identitySubject != null && this.identitySubject.equals(subject);
    }

    /**
     * Actualiza la información personal del conductor incrementando la versión.
     */
    public void updatePersonalInfo(PersonalInfo newInfo, Instant now) {
        this.personalInfo = Objects.requireNonNull(newInfo, "La nueva información personal no puede ser nula");
        this.updatedAt = Objects.requireNonNull(now, "updatedAt es requerido");
        this.version++;
    }

    /**
     * Transiciona el estado de habilitación con su motivo correspondiente e incrementa la versión.
     */
    public void updateQualification(QualificationInfo newQualification, Instant now) {
        this.qualification = Objects.requireNonNull(newQualification, "newQualification es requerido");
        this.updatedAt = Objects.requireNonNull(now, "updatedAt es requerido");
        this.version++;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Driver driver = (Driver) o;
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
                ", fullName='" + personalInfo.getFullName() + '\'' +
                ", status=" + getStatus() +
                ", version=" + version +
                '}';
    }
}
