package ar.edu.utn.frre.m3.application.usecase;

import ar.edu.utn.frre.m3.domain.ServiceType;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record RegisterDriverLicenseCommand(UUID driverId, String documentNumber,
                                           Instant issuedAt, Instant validUntil,
                                           Set<ServiceType> serviceTypes,
                                           String storageReference, long expectedVersion,
                                           String idempotencyKey, String requestHash) {
    public RegisterDriverLicenseCommand(UUID driverId, String documentNumber,
                                        Instant issuedAt, Instant validUntil,
                                        Set<ServiceType> serviceTypes,
                                        String storageReference, long expectedVersion) {
        this(driverId, documentNumber, issuedAt, validUntil, serviceTypes,
                storageReference, expectedVersion, null, null);
    }
}
