package ar.edu.utn.frre.m3.adapter.rest;

import ar.edu.utn.frre.m3.domain.Driver;
import ar.edu.utn.frre.m3.domain.DriverStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO de respuesta para la consulta de perfil de conductor (HU-02).
 * Expone la información necesaria sin filtrar detalles internos de persistencia.
 */
public record DriverProfileResponse(
        UUID id,
        String identitySubject,
        PersonalInfoResponse personalInfo,
        DriverStatus status,
        String statusDescription,
        String qualificationReason,
        boolean operable,
        int version,
        Instant createdAt,
        Instant updatedAt
) {
    public static DriverProfileResponse from(Driver driver) {
        if (driver == null) return null;
        return new DriverProfileResponse(
                driver.getId(),
                driver.getIdentitySubject(),
                PersonalInfoResponse.from(driver.getPersonalInfo()),
                driver.getStatus(),
                driver.getStatus().getDescription(),
                driver.getQualificationReason(),
                driver.isOperable(),
                driver.getVersion(),
                driver.getCreatedAt(),
                driver.getUpdatedAt()
        );
    }
}
