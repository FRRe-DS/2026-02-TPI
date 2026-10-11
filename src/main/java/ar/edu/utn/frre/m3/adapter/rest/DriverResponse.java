package ar.edu.utn.frre.m3.adapter.rest;

import ar.edu.utn.frre.m3.domain.Driver;
import ar.edu.utn.frre.m3.domain.QualificationStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO de respuesta para la representación de un conductor en la API REST.
 */
public record DriverResponse(
        UUID id,
        String identitySubject,
        String name,
        QualificationStatus qualificationStatus,
        int version,
        Instant createdAt,
        Instant updatedAt
) {
    public static DriverResponse fromDomain(Driver driver) {
        return new DriverResponse(
                driver.getId(),
                driver.getIdentitySubject(),
                driver.getName(),
                driver.getQualificationStatus(),
                driver.getVersion(),
                driver.getCreatedAt(),
                driver.getUpdatedAt()
        );
    }
}
